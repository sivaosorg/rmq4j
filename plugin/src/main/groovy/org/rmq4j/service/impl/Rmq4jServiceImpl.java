package org.rmq4j.service.impl;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.common.Rmq4j;
import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jService;
import org.rmq4j.service.Rmq4jWrapCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
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
     * Retrieves a list of all enabled RabbitMQ configurations.
     * <p>
     * This method first checks if there are any available configurations by calling {@link #isAvailableConfigs()}.
     * If no configurations are available, it returns an empty list.
     * If configurations are available, it filters the configurations to select only those that are enabled.
     * The method then returns a list of enabled {@link Rmq4jProperties.Config} objects.
     * <p>
     * This is useful for retrieving configurations that are currently active and should be applied for RabbitMQ setups.
     *
     * @return A list of enabled {@link Rmq4jProperties.Config} objects. If no configurations are available or none are enabled,
     * an empty list is returned.
     */
    @Override
    public List<Rmq4jProperties.Config> getConfigsActivated(String clusterKey) {
        if (!this.isAvailableConfigs()) {
            return Collections.emptyList();
        }
        return properties.getConfigs()
                .stream()
                .filter(e -> e.isEnabled() && e.getClusterKey().equals(clusterKey))
                .collect(Collectors.toList());
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
        return this.getConfigsActivated(clusterKey).stream().findFirst();
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
        if (String4j.isEmpty(config.getExchange().getName())) {
            throw new IllegalArgumentException("Rmq4j, invalid name of exchange");
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

    /**
     * Creates a RabbitMQ queue based on the provided configuration.
     * <p>
     * This method constructs a RabbitMQ queue using the parameters defined in the provided {@link Rmq4jProperties.Config}.
     * If the configuration or the queue details within it are null, the method returns an empty {@link Optional}.
     * <p>
     * The method first validates the queue name by checking if the exchange name in the configuration is empty.
     * If the exchange name is invalid (empty or null), it throws an {@link IllegalArgumentException}.
     * <p>
     * After validating the input, the method creates a new {@link Queue} instance using the queue name, durability,
     * exclusivity, and auto-delete properties specified in the configuration. If any custom arguments are provided,
     * they are added to the queue using the {@link Queue#addArgument(String, Object)} method.
     * <p>
     * Finally, the queue is returned wrapped in an {@link Optional}.
     *
     * @param config The configuration object containing the details needed to create the RabbitMQ queue.
     * @return An {@link Optional} containing the created {@link Queue} if the configuration is valid; otherwise,
     * an empty {@link Optional}.
     * @throws IllegalArgumentException if the queue name is not specified or is invalid.
     */
    @Override
    public Optional<Queue> createQueue(Rmq4jProperties.Config config) {
        if (config == null || config.getQueue() == null) {
            return Optional.empty();
        }
        if (String4j.isEmpty(config.getExchange().getName())) {
            throw new IllegalArgumentException("Rmq4j, invalid name of queue");
        }
        Rmq4jProperties.Queue queue = config.getQueue();
        Queue q = new Queue(queue.getName(), queue.isDurable(), queue.isExclusive(), queue.isClearable());
        if (Collection4j.isNotEmptyMap(queue.getArguments())) {
            for (Map.Entry<String, Object> entry : queue.getArguments().entrySet()) {
                q.addArgument(entry.getKey(), entry.getValue());
            }
        }
        return Optional.of(q);
    }

    /**
     * Creates a RabbitMQ queue based on the provided configuration and invokes a callback upon completion.
     * <p>
     * This method first attempts to create a RabbitMQ queue using the provided {@link Rmq4jProperties.Config} by calling
     * {@link #createQueue(Rmq4jProperties.Config)}. If the queue creation is successful, the queue is returned wrapped in
     * an {@link Optional}.
     * <p>
     * If the queue creation fails due to any exception, the method catches the exception and builds an error response using
     * {@link HttpWrapBuilder}. The error response includes details such as the status code, error message, and the configuration
     * details that caused the failure. This information is then passed to the provided {@link Rmq4jWrapCallback} if it is not null.
     * <p>
     * Finally, the callback's {@code onCallback} method is invoked with the response, whether the queue creation succeeded or failed.
     *
     * @param config   The configuration object containing the details needed to create the RabbitMQ queue.
     * @param callback The callback interface to be invoked with the result of the queue creation process.
     * @return An {@link Optional} containing the created {@link Queue} if the configuration is valid and the queue creation
     * succeeds; otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Queue> createQueue(Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        Optional<Queue> queue = Optional.empty();
        try {
            queue = this.createQueue(config);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating queue failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return queue;
    }

    /**
     * Creates a RabbitMQ binding based on the provided configuration.
     * <p>
     * This method constructs a RabbitMQ binding by first creating a queue and an exchange using the provided
     * {@link Rmq4jProperties.Config}, and then binding the queue to the exchange with a specified routing key.
     * <p>
     * If the configuration or the binding details within it are null, the method returns an empty {@link Optional}.
     * <p>
     * The method first validates the input configuration to ensure that it is not null and that it contains valid binding
     * details. If any of these checks fail, an empty {@link Optional} is returned.
     * <p>
     * The method then attempts to create the necessary queue and exchange using the {@link #createQueue(Rmq4jProperties.Config)}
     * and {@link #createExchange(Rmq4jProperties.Config)} methods. It uses the {@link BindingBuilder} to bind the created queue
     * to the exchange using the routing key provided in the configuration. The binding is created without additional arguments by default.
     * <p>
     * If the binding configuration includes custom arguments, they are added to the binding using the {@link Binding#addArgument(String, Object)}
     * method.
     * <p>
     * Finally, the binding is returned wrapped in an {@link Optional}. If any step fails or the binding cannot be created,
     * an empty {@link Optional} is returned.
     *
     * @param config The configuration object containing the details needed to create the RabbitMQ binding.
     * @return An {@link Optional} containing the created {@link Binding} if the configuration is valid; otherwise,
     * an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalGetWithoutIsPresent"})
    @Override
    public Optional<Binding> createBinding(Rmq4jProperties.Config config) {
        if (config == null || config.getBind() == null) {
            return Optional.empty();
        }
        Rmq4jProperties.Bind bind = config.getBind();
        Binding binding = BindingBuilder
                .bind(this.createQueue(config).get())
                .to(this.createExchange(config).get())
                .with(bind.getRoutingKey())
                .noargs();
        if (Collection4j.isNotEmptyMap(binding.getArguments())) {
            for (Map.Entry<String, Object> entry : bind.getArguments().entrySet()) {
                binding.addArgument(entry.getKey(), entry.getValue());
            }
        }
        return Optional.of(binding);
    }

    /**
     * Creates a RabbitMQ binding based on the provided configuration and executes a callback upon completion.
     * <p>
     * This method constructs a RabbitMQ binding by first creating a queue and an exchange using the provided
     * {@link Rmq4jProperties.Config}, and then binding the queue to the exchange with a specified routing key.
     * If the binding creation succeeds or fails, the method invokes the provided {@link Rmq4jWrapCallback} with
     * the appropriate response, which includes details about the success or failure of the operation.
     * <p>
     * If the configuration or the binding details within it are null, the method returns an empty {@link Optional}
     * and sends a failure response via the callback.
     * <p>
     * The method first validates the input configuration to ensure that it is not null and that it contains valid binding
     * details. If any of these checks fail, an empty {@link Optional} is returned, and an error message is added to the
     * callback response.
     * <p>
     * The method then attempts to create the necessary queue and exchange using the {@link #createQueue(Rmq4jProperties.Config)}
     * and {@link #createExchange(Rmq4jProperties.Config)} methods. It uses the {@link BindingBuilder} to bind the created queue
     * to the exchange using the routing key provided in the configuration. The binding is created without additional arguments by default.
     * <p>
     * If the binding configuration includes custom arguments, they are added to the binding using the {@link Binding#addArgument(String, Object)}
     * method.
     * <p>
     * If any exception occurs during the binding creation process, the method catches the exception, logs the error details,
     * and includes them in the callback response. The callback is then executed with the constructed response.
     * <p>
     * Finally, the created binding is returned wrapped in an {@link Optional}. If the binding cannot be created,
     * an empty {@link Optional} is returned.
     *
     * @param config   The configuration object containing the details needed to create the RabbitMQ binding.
     * @param callback The callback to be executed after the binding creation process is completed. The callback
     *                 will receive a response indicating whether the binding creation was successful or if it failed.
     * @return An {@link Optional} containing the created {@link Binding} if the configuration is valid; otherwise,
     * an empty {@link Optional}.
     */
    @Override
    public Optional<Binding> createBinding(Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        Optional<Binding> bind = Optional.empty();
        try {
            bind = this.createBinding(config);
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, creating binding failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return bind;
    }

    /**
     * Executes the RabbitMQ configuration using the provided connection and configuration details.
     * <p>
     * This method first creates a {@link CachingConnectionFactory} using the provided {@link Rmq4jProperties.Connection}.
     * If the connection factory is successfully created, it calls {@link #executeConfig(CachingConnectionFactory, Rmq4jProperties.Config)}
     * to apply the RabbitMQ configuration using the created connection factory.
     * <p>
     * If the connection factory cannot be created, the method returns without executing any further actions.
     *
     * @param connection The connection details required to create a {@link CachingConnectionFactory}.
     * @param config     The configuration object containing the details needed to configure RabbitMQ.
     */
    @Override
    public void executeConfig(Rmq4jProperties.Connection connection, Rmq4jProperties.Config config) {
        Optional<CachingConnectionFactory> factory = this.createCacheConnFactory(connection);
        if (!factory.isPresent()) {
            return;
        }
        this.executeConfig(factory.get(), config);
    }

    /**
     * Executes the RabbitMQ configuration using the provided connection details and configuration details, and invokes a callback
     * upon completion.
     * <p>
     * This method first creates a {@link CachingConnectionFactory} using the provided {@link Rmq4jProperties.Connection} details.
     * If the connection factory is successfully created, it then calls {@link #executeConfig(CachingConnectionFactory, Rmq4jProperties.Config)}
     * to apply the RabbitMQ configuration using the created connection factory.
     * <p>
     * If any exception occurs during the configuration process, the method captures the error, builds a response indicating the failure,
     * and includes details about the error and configuration in the response. The provided {@link Rmq4jWrapCallback} is then invoked
     * with the constructed response.
     * <p>
     * If the connection factory cannot be created, the method returns without further action and does not invoke the callback.
     * <p>
     * The method returns a boolean indicating whether the configuration execution was successful.
     *
     * @param connection The connection details required to create a {@link CachingConnectionFactory}.
     * @param config     The configuration object containing the details needed to configure RabbitMQ.
     * @param callback   The callback to be executed after the configuration process is completed. The callback
     *                   will receive a response indicating whether the configuration execution was successful or if it failed.
     * @return A boolean value indicating whether the configuration execution was successful. Returns `true` if successful,
     * `false` otherwise.
     */
    @Override
    public boolean executeConfig(Rmq4jProperties.Connection connection, Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        boolean success = false;
        try {
            this.executeConfig(connection, config);
            success = true;
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, executing config (exchange, queue, binding) failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return success;
    }

    /**
     * Executes the RabbitMQ configuration using the provided {@link CachingConnectionFactory} and configuration details.
     * <p>
     * This method creates a {@link RabbitAdmin} using the provided {@link CachingConnectionFactory} and then calls
     * {@link #executeConfig(RabbitAdmin, Rmq4jProperties.Config)} to apply the RabbitMQ configuration.
     * <p>
     * If the provided factory is null, the method returns without executing any further actions.
     *
     * @param factory The {@link CachingConnectionFactory} used to create the {@link RabbitAdmin}.
     * @param config  The configuration object containing the details needed to configure RabbitMQ.
     */
    @Override
    public void executeConfig(CachingConnectionFactory factory, Rmq4jProperties.Config config) {
        if (factory == null) {
            return;
        }
        RabbitAdmin adm = new RabbitAdmin(factory);
        this.executeConfig(adm, config);
    }

    /**
     * Executes the RabbitMQ configuration using the provided {@link CachingConnectionFactory}, configuration details,
     * and callback.
     * <p>
     * This method creates a {@link RabbitAdmin} instance using the provided {@link CachingConnectionFactory} and then
     * applies the RabbitMQ configuration by calling {@link #executeConfig(RabbitAdmin, Rmq4jProperties.Config)}.
     * <p>
     * After attempting to apply the configuration, the method executes the provided {@link Rmq4jWrapCallback} with a
     * response indicating whether the configuration process was successful or if it failed.
     * <p>
     * If the provided factory is null, the method returns without performing any actions.
     * <p>
     * If the configuration application is successful, the method returns {@code true}. If an exception occurs during
     * the configuration process, the method catches the exception, logs the error details, updates the response to reflect
     * the failure, and returns {@code false}. The callback is executed with the constructed response regardless of success
     * or failure.
     *
     * @param factory  The {@link CachingConnectionFactory} used to create the {@link RabbitAdmin}.
     * @param config   The configuration object containing the details needed to configure RabbitMQ.
     * @param callback The callback to be executed after attempting to apply the configuration. The callback will receive
     *                 a response indicating whether the configuration process was successful or if it failed.
     * @return {@code true} if the configuration was successfully applied; {@code false} if an exception occurred.
     */
    @Override
    public boolean executeConfig(CachingConnectionFactory factory, Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        boolean success = false;
        try {
            this.executeConfig(factory, config);
            success = true;
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, executing config (exchange, queue, binding) failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return success;
    }

    /**
     * Executes the RabbitMQ configuration using the provided {@link RabbitAdmin} and configuration details.
     * <p>
     * This method applies the RabbitMQ configuration by declaring the exchange, queue, and binding as specified
     * in the provided {@link Rmq4jProperties.Config}.
     * <p>
     * The method first checks if the provided {@link RabbitAdmin} and configuration are valid (not null).
     * If the configuration is valid, it creates and declares the exchange, queue, and binding using the
     * corresponding methods: {@link #createExchange(Rmq4jProperties.Config)}, {@link #createQueue(Rmq4jProperties.Config)},
     * and {@link #createBinding(Rmq4jProperties.Config)}.
     * <p>
     * If any of these steps fail or the configuration is invalid, the method returns without performing any actions.
     *
     * @param adm    The {@link RabbitAdmin} instance used to declare the exchange, queue, and binding.
     * @param config The configuration object containing the details needed to configure RabbitMQ.
     */
    @Override
    public void executeConfig(RabbitAdmin adm, Rmq4jProperties.Config config) {
        if (adm == null || config == null || config.getExchange() == null ||
                config.getQueue() == null || config.getBind() == null) {
            return;
        }
        // declaring exchange
        Optional<Exchange> exchange = this.createExchange(config);
        exchange.ifPresent(adm::declareExchange);

        // declaring queue
        Optional<Queue> queue = this.createQueue(config);
        queue.ifPresent(adm::declareQueue);

        // declaring binding
        Optional<Binding> bind = this.createBinding(config);
        bind.ifPresent(adm::declareBinding);
    }

    /**
     * Executes the RabbitMQ configuration using the provided {@link RabbitAdmin}, configuration details, and callback.
     * <p>
     * This method applies the RabbitMQ configuration by declaring the exchange, queue, and binding as specified
     * in the provided {@link Rmq4jProperties.Config}. After attempting to apply the configuration, it executes the
     * provided {@link Rmq4jWrapCallback} with a response indicating the success or failure of the operation.
     * <p>
     * The method first creates a response object using {@link HttpWrapBuilder} with the provided configuration details
     * and the current session ID. It then attempts to apply the configuration by calling {@link #executeConfig(RabbitAdmin, Rmq4jProperties.Config)}.
     * <p>
     * If any exception occurs during the configuration process, the method catches the exception, logs the error details,
     * and updates the response to reflect the failure. The response includes a status code, error message, and details about
     * the configuration that was attempted.
     * <p>
     * Regardless of success or failure, the method ensures that the callback is executed with the constructed response.
     *
     * @param adm      The {@link RabbitAdmin} instance used to declare the exchange, queue, and binding.
     * @param config   The configuration object containing the details needed to configure RabbitMQ.
     * @param callback The callback to be executed after attempting to apply the configuration. The callback will receive
     *                 a response indicating whether the configuration process was successful or if it failed.
     */
    @Override
    public boolean executeConfig(RabbitAdmin adm, Rmq4jProperties.Config config, Rmq4jWrapCallback callback) {
        HttpWrapBuilder<?> response = new HttpWrapBuilder<>().ok(config).requestId(Rmq4j.getCurrentSessionId());
        boolean success = false;
        try {
            this.executeConfig(adm, config);
            success = true;
        } catch (Exception e) {
            response
                    .statusCode(HttpStatusBuilder.INTERNAL_SERVER_ERROR)
                    .message("Rmq4j, executing config (exchange, queue, binding) failed")
                    .debug("cause", e.getMessage())
                    .errors(e)
                    .customFields("config_details", Json4j.toJson(config));
        }
        if (callback != null) {
            callback.onCallback(response.build());
        }
        return success;
    }
}
