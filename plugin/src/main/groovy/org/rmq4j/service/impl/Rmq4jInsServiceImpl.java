package org.rmq4j.service.impl;

import org.rmq4j.common.Rmq4j;
import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jInsService;
import org.rmq4j.service.Rmq4jService;
import org.rmq4j.service.Rmq4jWrapCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unify4j.common.Collection4j;
import org.unify4j.common.Json4j;
import org.unify4j.common.String4j;
import org.unify4j.model.builder.HttpStatusBuilder;
import org.unify4j.model.builder.HttpWrapBuilder;
import org.unify4j.model.enums.IconType;
import org.unify4j.model.response.WrapResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({"FieldCanBeLocal", "DuplicatedCode"})
@Service
public class Rmq4jInsServiceImpl implements Rmq4jInsService {
    protected static final Logger logger = LoggerFactory.getLogger(Rmq4jInsServiceImpl.class);
    protected final Map<String, CachingConnectionFactory> factories = new ConcurrentHashMap<>();
    protected final Map<String, RabbitTemplate> templates = new ConcurrentHashMap<>();
    protected final Lock lock = new ReentrantLock();
    protected final Rmq4jService rmq4jService;

    @Autowired
    public Rmq4jInsServiceImpl(
            Rmq4jService rmq4jService) {
        this.rmq4jService = rmq4jService;
    }

    /**
     * Initializes and caches RabbitMQ connections and templates for the configured clusters.
     * <p>
     * This method checks if the RabbitMQ service is enabled and if the necessary configurations exist.
     * For each enabled cluster configuration, it creates a {@link CachingConnectionFactory} and a {@link RabbitTemplate}.
     * These are then stored in the internal maps {@link #factories} and {@link #templates}, respectively, for later use.
     * <p>
     * If the service is not enabled or the cluster configuration is invalid, the method returns without taking action.
     */
    @Override
    public void snapIns() {
        this.snapIns(new Rmq4jWrapCallback() {
            @Override
            public void onCallback(WrapResponse<?> response) {
                if (response.isSuccess()) {
                    logger.info("{} Rmq4j, connected successfully ({})", IconType.SUCCESS.getCode(), response.getStatusCode());
                }
                if (response.isError()) {
                    logger.error("{} ({}) {}, {}", IconType.ERROR.getCode(), response.getStatusCode(),
                            response.getMessage(), Json4j.toJson(response.getDebug()));
                }
            }
        });
    }

    /**
     * Initializes and caches RabbitMQ connections and templates for the configured clusters with callback handling.
     * <p>
     * This method first attempts to initialize the RabbitMQ connections and templates by calling the no-argument
     * {@link #snapIns()} method. It creates an HTTP response builder with an initial "OK" status and the current
     * session ID, which will be used to generate a response once the operation completes.
     * <p>
     * If the initialization fails due to an exception, the response builder is updated with an internal server error
     * status, an error message, and additional debug information. The caught exception is also included in the response
     * for detailed error tracking.
     * <p>
     * After the initialization attempt, if a {@link Rmq4jWrapCallback} is provided, it invokes the callback's
     * {@code onCallback} method with the built HTTP response, which allows the caller to handle the outcome of the
     * initialization process asynchronously.
     *
     * @param callback An optional {@link Rmq4jWrapCallback} that is triggered after the RabbitMQ connections and templates
     *                 are initialized. The callback receives an HTTP response object that contains the result of the
     *                 operation, including status and error details if any issues occurred.
     */
    @Override
    public void snapIns(Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>()
                .ok(rmq4jService.getConnections())
                .requestId(Rmq4j.getCurrentSessionId())
                .debug("all_connections_activated", Json4j.toJson(rmq4jService.getConnectionsActivated()));
        if (!rmq4jService.isEnabled()) {
            response
                    .statusCode(HttpStatusBuilder.SERVICE_UNAVAILABLE)
                    .message("Rmq4j, Service unavailable");
            if (callback != null) {
                callback.onCallback(response.build());
            }
            return;
        }
        if (this.exists()) {
            response.statusCode(HttpStatusBuilder.IM_USED);
            if (callback != null) {
                callback.onCallback(response.build());
            }
            return;
        }
        lock.lock();
        try {
            for (Map.Entry<String, Rmq4jProperties.Connection> entry : rmq4jService.getConnectionsActivated().entrySet()) {
                Optional<CachingConnectionFactory> factory = rmq4jService.createCacheConnFactory(entry.getValue(), callback);
                if (!factory.isPresent()) {
                    break;
                }
                Optional<RabbitTemplate> template = rmq4jService.dispatch(factory.get(), null);
                if (!template.isPresent()) {
                    break;
                }
                List<Rmq4jProperties.Config> configs = rmq4jService.getConfigsActivated(entry.getKey());
                if (Collection4j.isNotEmpty(configs)) {
                    boolean stopped = false;
                    for (Rmq4jProperties.Config e : configs) {
                        boolean success = rmq4jService.executeConfig(factory.get(), e, callback);
                        if (!success) {
                            stopped = true;
                            break;
                        }
                    }
                    if (stopped) {
                        break;
                    }
                }
                factories.put(entry.getKey(), factory.get());
                templates.put(entry.getKey(), template.get());
            }
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating multiples RabbitMQ connection failed")
                    .debug("cause", e.getMessage())
                    .errors(e);
        } finally {
            lock.unlock();
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
    }

    /**
     * Checks if there are any cached RabbitMQ connection factories and templates.
     * <p>
     * This method verifies the existence of both cached connection factories in {@link #factories}
     * and templates in {@link #templates}. It returns true if both maps contain entries,
     * indicating that the service has been initialized successfully.
     *
     * @return {@code true} if there are cached connection factories and templates; {@code false} otherwise.
     */
    @Override
    public boolean exists() {
        return Collection4j.isNotEmptyMap(factories) && Collection4j.isNotEmptyMap(templates);
    }

    /**
     * Retrieves the cached {@link CachingConnectionFactory} associated with the specified key.
     * <p>
     * This method returns the {@link CachingConnectionFactory} corresponding to the provided key,
     * if it exists in the cache. If the key is empty, the service is not initialized, or the key
     * does not exist in the cache, an empty {@link Optional} is returned.
     *
     * @param key The key used to identify the desired {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link CachingConnectionFactory} if found; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<CachingConnectionFactory> getWorker(String key) {
        if (String4j.isEmpty(key) || !this.exists() || !factories.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(factories.get(key));
    }

    /**
     * Retrieves the cached {@link RabbitTemplate} associated with the specified key.
     * <p>
     * This method returns the {@link RabbitTemplate} corresponding to the provided key,
     * if it exists in the cache. If the key is empty, the service is not initialized, or the key
     * does not exist in the cache, an empty {@link Optional} is returned.
     *
     * @param key The key used to identify the desired {@link RabbitTemplate}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if found; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<RabbitTemplate> getDispatcher(String key) {
        if (String4j.isEmpty(key) || !this.exists() || !templates.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(templates.get(key));
    }

    /**
     * Publishes a message using the {@link RabbitTemplate} associated with the specified key.
     * <p>
     * This method retrieves the {@link RabbitTemplate} corresponding to the given key
     * by invoking {@link #getDispatcher(String)}. If a valid {@link RabbitTemplate} is found,
     * the method proceeds to publish the provided message using that template. If the key
     * is invalid or if no {@link RabbitTemplate} is associated with the key, the method returns
     * without performing any operation.
     *
     * @param key     The key used to identify the {@link RabbitTemplate} for message publishing.
     * @param message The message to be published using the {@link RabbitTemplate}.
     */
    @Override
    public void publish(String key, Object message) {
        Optional<RabbitTemplate> template = this.getDispatcher(key);
        if (!template.isPresent()) {
            return;
        }
        // do something
    }
}
