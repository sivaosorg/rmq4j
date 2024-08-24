package org.rmq4j.service.impl;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.common.Rmq4j;
import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jService;
import org.rmq4j.service.Rmq4jWrapCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unify4j.common.Collection4j;
import org.unify4j.common.Json4j;
import org.unify4j.common.String4j;
import org.unify4j.model.builder.HttpStatusBuilder;
import org.unify4j.model.builder.HttpWrapBuilder;
import org.unify4j.model.enums.IconType;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal", "DuplicatedCode"})
@Service
public class Rmq4jServiceImpl implements Rmq4jService {
    protected static final Logger logger = LoggerFactory.getLogger(Rmq4jServiceImpl.class);

    protected final Rmq4jProperties properties;

    @Autowired
    public Rmq4jServiceImpl(Rmq4jProperties properties) {
        this.properties = properties;
    }

    /**
     * Checks if the rmq4j module is globally enabled and if there are any defined clusters.
     *
     * @return true if the rmq4j module is enabled and there is at least one cluster defined; false otherwise.
     */
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && Collection4j.isNotEmptyMap(properties.getClusters());
    }

    /**
     * Determines if debugging is enabled for the rmq4j module.
     *
     * @return true if debugging is enabled; false otherwise.
     */
    @Override
    public boolean isDebugging() {
        return properties.isDebugging();
    }

    /**
     * @return true if the Rmq4j has been configured the collection of exchange, queue and binding, false otherwise
     */
    @Override
    public boolean isAvailableConfigs() {
        return this.isEnabled() && Collection4j.isNotEmpty(properties.getConfigs());
    }

    /**
     * @return map of cluster configuration, class {@link Map}
     */
    @Override
    public Map<String, Rmq4jProperties.Connection> getConnections() {
        return properties.getClusters();
    }

    /**
     * Retrieves a specific cluster configuration based on the provided key.
     * <p>
     * This method checks if the `rmq4j` module is enabled and if the provided key is not empty.
     * If both conditions are met, it searches the clusters map for the entry that matches the given key
     * and returns an `Optional` containing the cluster configuration if found.
     * If the module is not enabled or the key is empty, it returns an empty `Optional`.
     *
     * @param key The key (cluster name) used to look up the cluster configuration in the clusters map.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Connection} associated with the given key if it exists;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Connection> getConn(String key) {
        if (!this.isEnabled() || String4j.isEmpty(key)) {
            return Optional.empty();
        }
        return this.getConnections().entrySet().stream().filter(e -> e.getKey().equals(key)).map(Map.Entry::getValue).findFirst();
    }

    /**
     * Retrieves a specific cluster configuration based on the provided key, but only if the cluster is enabled.
     * <p>
     * This method first calls {@link #getConn(String)} to retrieve the cluster configuration for the given key.
     * If the cluster configuration is present, it further checks if the cluster is enabled.
     * If the cluster is enabled, it returns the configuration wrapped in an {@link Optional}.
     * If the configuration is not present or the cluster is not enabled, it returns an empty {@link Optional}.
     *
     * @param key The key (cluster name) used to look up the cluster configuration in the clusters map.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Connection} associated with the given key if it exists and is enabled;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Connection> getConnActivated(String key) {
        Optional<Rmq4jProperties.Connection> cluster = this.getConn(key);
        if (!cluster.isPresent()) {
            return cluster;
        }
        return cluster.get().isEnabled() ? cluster : Optional.empty();
    }

    /**
     * Retrieves a map of all enabled RabbitMQ cluster configurations.
     * <p>
     * This method first checks if the RabbitMQ service is enabled by calling {@link #isEnabled()}.
     * If the service is not enabled, it returns an empty map.
     * If the service is enabled, it iterates through all the available cluster configurations retrieved via
     * {@link #getConnections()}.
     * For each configuration, it checks if the cluster is enabled.
     * Only the enabled clusters added to a new map, which is then returned.
     *
     * @return A map containing the names and {@link Rmq4jProperties.Connection} objects of all enabled RabbitMQ clusters.
     * If the service is not enabled or no clusters are enabled, an empty map is returned.
     */
    @Override
    public Map<String, Rmq4jProperties.Connection> getConnectionsActivated() {
        if (!this.isEnabled()) {
            return Collections.emptyMap();
        }
        Map<String, Rmq4jProperties.Connection> connections = new HashMap<>();
        for (Map.Entry<String, Rmq4jProperties.Connection> entry : this.getConnections().entrySet()) {
            if (!entry.getValue().isEnabled()) {
                continue;
            }
            connections.put(entry.getKey(), entry.getValue());
        }
        return connections;
    }

    /**
     * Retrieves a list of all enabled RabbitMQ configurations.
     * <p>
     * This method checks if any configurations are available by calling {@link #isAvailableConfigs()}.
     * If no configurations are available, it returns an empty list.
     * If configurations are available, it filters the configurations, selecting only those that are enabled.
     * The method returns a list of enabled {@link Rmq4jProperties.Config} objects.
     *
     * @return A list of enabled {@link Rmq4jProperties.Config} objects. If no configurations are available or enabled,
     * an empty list is returned.
     */
    @Override
    public List<Rmq4jProperties.Config> getConfigsActivated() {
        if (!this.isAvailableConfigs()) {
            return Collections.emptyList();
        }
        return properties.getConfigs().stream().filter(Rmq4jProperties.Config::isEnabled).collect(Collectors.toList());
    }

    /**
     * Retrieves the configuration for a specific RabbitMQ cluster identified by the provided key, but only if the
     * configuration is enabled.
     * <p>
     * This method first checks if the provided cluster key is empty using {@link String4j#isEmpty(CharSequence)}.
     * If the key is empty, it returns an empty {@link Optional}.
     * If the key is valid, it filters through the list of enabled configurations retrieved via
     * {@link #getConfigsActivated()}, looking for a configuration that matches the provided cluster key.
     * If a matching configuration is found, it is returned wrapped in an {@link Optional}.
     * If no matching configuration is found, an empty {@link Optional} is returned.
     *
     * @param clusterKey The key identifying the RabbitMQ cluster whose configuration is to be retrieved.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Config} associated with the given cluster key
     * if it exists and is enabled; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Config> getConfig(String clusterKey) {
        if (String4j.isEmpty(clusterKey)) {
            return Optional.empty();
        }
        return this.getConfigsActivated().stream().filter(e -> e.getClusterKey().equals(clusterKey)).findFirst();
    }

    /**
     * Retrieves the configuration for a specific RabbitMQ cluster identified by the provided cluster key and dispatch key,
     * but only if the configuration is enabled.
     * <p>
     * This method first checks if either the provided cluster key or dispatch key is empty using {@link String4j#isEmpty(CharSequence)}.
     * If either key is empty, it returns an empty {@link Optional}.
     * If both keys are valid, it filters through the list of enabled configurations retrieved via {@link #getConfigsActivated()},
     * looking for a configuration that matches both the provided cluster key and dispatch key.
     * If a matching configuration is found, it is returned wrapped in an {@link Optional}.
     * If no matching configuration is found, an empty {@link Optional} is returned.
     *
     * @param clusterKey  The key identifying the RabbitMQ cluster whose configuration is to be retrieved.
     * @param dispatchKey The key identifying the specific dispatch configuration within the cluster.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Config} associated with the given cluster key
     * and dispatch key if it exists and is enabled; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Config> getConfig(String clusterKey, String dispatchKey) {
        if (String4j.isEmpty(clusterKey) || String4j.isEmpty(dispatchKey)) {
            return Optional.empty();
        }
        return this.getConfigsActivated().stream()
                .filter(e -> e.getClusterKey().equals(clusterKey) && e.getDispatchKey().equals(dispatchKey))
                .findFirst();
    }

    /**
     * Creates a {@link ConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method initializes a {@link ConnectionFactory} using the details from the given {@link Rmq4jProperties.Connection}.
     * It sets the host, port, virtual host, username, and password on the factory. If SSL is enabled in the configuration,
     * it applies SSL settings to the factory.
     * <p>
     * If the configuration is null or the cluster is not enabled, it returns an empty {@link Optional}.
     * If an exception occurs while applying SSL settings, it logs the error and throws a {@link RuntimeException}.
     *
     * @param connection The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the configured {@link ConnectionFactory} if the configuration is valid and SSL settings are applied successfully;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<ConnectionFactory> createFactory(Rmq4jProperties.Connection connection) {
        if (!this.isEnabled() || connection == null) {
            return Optional.empty();
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(connection.getHost());
        factory.setPort(connection.getPort());
        factory.setVirtualHost(connection.getVirtualHost());
        factory.setUsername(connection.getUsername());
        factory.setPassword(connection.getPassword());
        try {
            if (connection.isUseSSL()) {
                factory.useSslProtocol();
            }
            return Optional.of(factory);
        } catch (Exception e) {
            logger.error("{} Rmq4j, SSL configuration for RabbitMQ could not be applied cause got an exception: {} by URL schema: {}",
                    IconType.ERROR.getCode(), e.getMessage(), this.getURLConnSchema(connection), e);
            throw new RuntimeException(String.format("SSL configuration for RabbitMQ could not be applied successfully (%s)", this.getURLConnSchema(connection)), e);
        }
    }

    /**
     * Creates a {@link ConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method initializes a {@link ConnectionFactory} using the details from the given {@link Rmq4jProperties.Connection}.
     * It sets the host, port, virtual host, username, and password on the factory. If SSL is enabled in the configuration,
     * it applies SSL settings to the factory.
     * <p>
     * If the configuration is null or the cluster is not enabled, it returns an empty {@link Optional}.
     * If an exception occurs while applying SSL settings, it logs the error and throws a {@link RuntimeException}.
     *
     * @param connection The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the configured {@link ConnectionFactory} if the configuration is valid and SSL settings are applied successfully;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<ConnectionFactory> createFactory(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(null).requestId(Rmq4j.getCurrentSessionId());
        Optional<ConnectionFactory> factory = Optional.empty();
        try {
            factory = this.createFactory(connection);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating connection factory failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("conn_string", Json4j.toJson(connection));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return factory;
    }

    /**
     * Creates a {@link CachingConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createFactory(Rmq4jProperties.Connection)} to create a {@link ConnectionFactory}
     * from the given cluster configuration. If the factory creation is successful, it wraps the factory in a
     * {@link CachingConnectionFactory} and returns it as an {@link Optional}.
     * <p>
     * If the {@link ConnectionFactory} could not be created (e.g., due to invalid configuration), it returns
     * an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the {@link CachingConnectionFactory} if the {@link ConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Connection connection) {
        Optional<ConnectionFactory> factory = this.createFactory(connection);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new CachingConnectionFactory(factory.get()));
    }

    /**
     * Creates a {@link CachingConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createFactory(Rmq4jProperties.Connection)} to create a {@link ConnectionFactory}
     * from the given cluster configuration. If the factory creation is successful, it wraps the factory in a
     * {@link CachingConnectionFactory} and returns it as an {@link Optional}.
     * <p>
     * If the {@link ConnectionFactory} could not be created (e.g., due to invalid configuration), it returns
     * an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the {@link CachingConnectionFactory} if the {@link ConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(null).requestId(Rmq4j.getCurrentSessionId());
        Optional<CachingConnectionFactory> factory = Optional.empty();
        try {
            factory = this.createCacheConnFactory(connection);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating cache connection factory failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("conn_string", Json4j.toJson(connection));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return factory;
    }

    /**
     * Creates a {@link RabbitTemplate} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitTemplate}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<RabbitTemplate> dispatch(Rmq4jProperties.Connection connection) {
        Optional<CachingConnectionFactory> factory = this.createCacheConnFactory(connection);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return this.dispatch(factory.get());
    }

    /**
     * Creates a {@link RabbitTemplate} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitTemplate}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<RabbitTemplate> dispatch(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(null).requestId(Rmq4j.getCurrentSessionId());
        Optional<RabbitTemplate> template = Optional.empty();
        try {
            template = this.dispatch(connection);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating RabbitMQ Template failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("conn_string", Json4j.toJson(connection));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return template;
    }

    /**
     * Creates a {@link RabbitTemplate} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitTemplate}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param factory The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<RabbitTemplate> dispatch(CachingConnectionFactory factory) {
        if (!this.isEnabled() || factory == null) {
            return Optional.empty();
        }
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return Optional.of(template);
    }

    /**
     * Creates a {@link RabbitTemplate} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitTemplate}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param factory The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<RabbitTemplate> dispatch(CachingConnectionFactory factory, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(null).requestId(Rmq4j.getCurrentSessionId());
        Optional<RabbitTemplate> template = Optional.empty();
        try {
            template = this.dispatch(factory);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating RabbitMQ Template failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("cache_conn_string", factory != null ? factory.getCacheProperties().toString() : "undefined cache connection factory");
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return template;
    }

    /**
     * Creates a {@link RabbitAdmin} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitAdmin}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitAdmin} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<RabbitAdmin> createAdm(Rmq4jProperties.Connection connection) {
        Optional<CachingConnectionFactory> factory = this.createCacheConnFactory(connection);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new RabbitAdmin(factory.get()));
    }

    /**
     * Creates a {@link RabbitAdmin} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Connection)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitAdmin}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param connection The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitAdmin} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<RabbitAdmin> createAdm(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(null).requestId(Rmq4j.getCurrentSessionId());
        Optional<RabbitAdmin> adm = Optional.empty();
        try {
            adm = this.createAdm(connection);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating RabbitMQ Admin failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("conn_string", Json4j.toJson(connection));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return adm;
    }

    /**
     * Generates the connection URL schema for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method constructs the connection URL schema using the username, password, host, port, and virtual host from
     * the provided {@link Rmq4jProperties.Connection}. The URL is formatted as "amqp://username:password@host:port".
     * If a virtual host is specified, it is appended to the URL.
     * <p>
     * If the provided configuration is null, it returns an empty string.
     *
     * @param connection The cluster configuration used to build the connection URL.
     * @return A string representing the RabbitMQ connection URL schema based on the provided configuration.
     */
    @Override
    public String getURLConnSchema(Rmq4jProperties.Connection connection) {
        if (connection == null) {
            return "";
        }
        String form = String.format("amqp://%s:%s@%s:%d",
                connection.getUsername(), connection.getPassword(), connection.getHost(), connection.getPort());
        if (String4j.isEmpty(connection.getVirtualHost())) {
            return form;
        }
        return String.format("%s%s", form, connection.getVirtualHost());
    }

    /**
     * Creates a RabbitMQ exchange based on the provided configuration.
     * <p>
     * This method constructs a RabbitMQ exchange using the parameters defined in the provided {@link Rmq4jProperties.Config}.
     * If the configuration or the exchange details within it are null, the method returns an empty {@link Optional}.
     * <p>
     * The method first checks the type of exchange (e.g., "fanout", "topic", "headers", or "direct") specified in the
     * configuration. It uses the appropriate {@link ExchangeBuilder} method to create the corresponding exchange type.
     * If the exchange type is not specified or is invalid, an {@link IllegalArgumentException} is thrown.
     * <p>
     * After determining the exchange type, the method configures additional exchange properties such as
     * auto-delete, internal, delayed, durability, and any custom arguments. These properties are applied to the
     * exchange using the {@link ExchangeBuilder}.
     * <p>
     * Finally, the exchange is built and returned wrapped in an {@link Optional}.
     *
     * @param config The configuration object containing the details needed to create the RabbitMQ exchange.
     * @return An {@link Optional} containing the created {@link Exchange} if the configuration is valid; otherwise,
     * an empty {@link Optional}.
     * @throws IllegalArgumentException if the exchange type is not specified or is invalid.
     */
    @SuppressWarnings({"SpellCheckingInspection", "ExtractMethodRecommender"})
    @Override
    public Optional<Exchange> createExchange(Rmq4jProperties.Config config) {
        if (config == null || config.getExchange() == null) {
            return Optional.empty();
        }
        if (String4j.isEmpty(config.getExchange().getKind())) {
            throw new IllegalArgumentException("Rmq4j, invalid type of exchange");
        }
        Rmq4jProperties.Exchange exchange = config.getExchange();
        ExchangeBuilder builder;
        switch (exchange.getKind().toLowerCase()) {
            case "fanout":
                builder = ExchangeBuilder.fanoutExchange(exchange.getName());
                break;
            case "topic":
                builder = ExchangeBuilder.topicExchange(exchange.getName());
                break;
            case "headers":
                builder = ExchangeBuilder.headersExchange(exchange.getName());
                break;
            case "direct":
            default:
                builder = ExchangeBuilder.directExchange(exchange.getName());
                break;
        }
        if (exchange.isClearable()) {
            builder.autoDelete();
        }
        if (exchange.isInternal()) {
            builder.internal();
        }
        if (exchange.isDelayable()) {
            builder.delayed();
        }
        if (Collection4j.isNotEmptyMap(exchange.getArguments())) {
            builder.withArguments(exchange.getArguments());
        }
        Exchange e = builder
                .durable(exchange.isDurable())
                .build();
        return Optional.of(e);
    }

    /**
     * Creates a RabbitMQ exchange based on the provided configuration and handles the result through a callback.
     * <p>
     * This method attempts to create a RabbitMQ exchange using the provided {@link Rmq4jProperties.Config} by calling
     * {@link #createExchange(Rmq4jProperties.Config)}. If the exchange is created successfully, it is returned wrapped in an {@link Optional}.
     * <p>
     * If an exception occurs during the exchange creation, the method constructs an error response using
     * {@link HttpWrapBuilder}, capturing details such as the status code, error message, exception details, and the
     * configuration that caused the failure. The error response is passed to the provided {@link Rmq4jWrapCallback}.
     * <p>
     * If the callback is not null, the method triggers the callback with the constructed response, allowing for
     * asynchronous handling of the result.
     *
     * @param config   The configuration object containing the details needed to create the RabbitMQ exchange.
     * @param callback The callback to handle the response after attempting to create the exchange.
     * @return An {@link Optional} containing the created {@link Exchange} if the configuration is valid and the creation
     * was successful; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Exchange> createExchange(Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        Optional<Exchange> exchange = Optional.empty();
        try {
            exchange = this.createExchange(config);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating exchange failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return exchange;
    }
}
