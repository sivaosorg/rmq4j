package org.rmq4j.service.impl;

import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jInsService;
import org.rmq4j.service.Rmq4jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unify4j.common.Collection4j;
import org.unify4j.common.String4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"FieldCanBeLocal", "DuplicatedCode"})
@Service
public class Rmq4jInsServiceImpl implements Rmq4jInsService {
    protected static final Logger logger = LoggerFactory.getLogger(Rmq4jInsServiceImpl.class);
    protected final Map<String, CachingConnectionFactory> factories = new ConcurrentHashMap<>();
    protected final Map<String, RabbitTemplate> templates = new ConcurrentHashMap<>();

    protected final Rmq4jProperties properties;
    protected final Rmq4jService rmq4jService;

    @Autowired
    public Rmq4jInsServiceImpl(Rmq4jProperties properties,
                               Rmq4jService rmq4jService) {
        this.properties = properties;
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
        if (!rmq4jService.isEnabled()) {
            return;
        }
        if (this.exists()) {
            return;
        }
        for (Map.Entry<String, Rmq4jProperties.Node> entry : properties.getClusters().entrySet()) {
            if (!entry.getValue().isEnabled()) {
                continue;
            }
            Optional<CachingConnectionFactory> factory = rmq4jService.createCacheConnFactory(entry.getValue());
            if (!factory.isPresent()) {
                continue;
            }
            Optional<RabbitTemplate> template = rmq4jService.dispatch(factory.get());
            if (!template.isPresent()) {
                continue;
            }
            factories.put(entry.getKey(), factory.get());
            templates.put(entry.getKey(), template.get());
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
}
