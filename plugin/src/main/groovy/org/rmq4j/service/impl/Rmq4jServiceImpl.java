package org.rmq4j.service.impl;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unify4j.common.Collection4j;
import org.unify4j.common.String4j;
import org.unify4j.model.enums.IconType;

import java.util.Map;
import java.util.Optional;

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
     * Retrieves a specific cluster configuration based on the provided key.
     * <p>
     * This method checks if the `rmq4j` module is enabled and if the provided key is not empty.
     * If both conditions are met, it searches the clusters map for the entry that matches the given key
     * and returns an `Optional` containing the cluster configuration if found.
     * If the module is not enabled or the key is empty, it returns an empty `Optional`.
     *
     * @param key The key (cluster name) used to look up the cluster configuration in the clusters map.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Node} associated with the given key if it exists;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Node> getNode(String key) {
        if (!this.isEnabled() || String4j.isEmpty(key)) {
            return Optional.empty();
        }
        return properties.getClusters().entrySet().stream().filter(e -> e.getKey().equals(key)).map(Map.Entry::getValue).findFirst();
    }

    /**
     * Retrieves a specific cluster configuration based on the provided key, but only if the cluster is enabled.
     * <p>
     * This method first calls {@link #getNode(String)} to retrieve the cluster configuration for the given key.
     * If the cluster configuration is present, it further checks if the cluster is enabled.
     * If the cluster is enabled, it returns the configuration wrapped in an {@link Optional}.
     * If the configuration is not present or the cluster is not enabled, it returns an empty {@link Optional}.
     *
     * @param key The key (cluster name) used to look up the cluster configuration in the clusters map.
     * @return An {@link Optional} containing the {@link Rmq4jProperties.Node} associated with the given key if it exists and is enabled;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<Rmq4jProperties.Node> getNodeActivated(String key) {
        Optional<Rmq4jProperties.Node> cluster = this.getNode(key);
        if (!cluster.isPresent()) {
            return cluster;
        }
        return cluster.get().isEnabled() ? cluster : Optional.empty();
    }

    /**
     * Creates a {@link ConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method initializes a {@link ConnectionFactory} using the details from the given {@link Rmq4jProperties.Node}.
     * It sets the host, port, virtual host, username, and password on the factory. If SSL is enabled in the configuration,
     * it applies SSL settings to the factory.
     * <p>
     * If the configuration is null or the cluster is not enabled, it returns an empty {@link Optional}.
     * If an exception occurs while applying SSL settings, it logs the error and throws a {@link RuntimeException}.
     *
     * @param node The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the configured {@link ConnectionFactory} if the configuration is valid and SSL settings are applied successfully;
     * otherwise, an empty {@link Optional}.
     */
    @Override
    public Optional<ConnectionFactory> createFactory(Rmq4jProperties.Node node) {
        if (node == null) {
            return Optional.empty();
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(node.getHost());
        factory.setPort(node.getPort());
        factory.setVirtualHost(node.getVirtualHost());
        factory.setUsername(node.getUsername());
        factory.setPassword(node.getPassword());
        try {
            if (node.isUseSSL()) {
                factory.useSslProtocol();
            }
            return Optional.of(factory);
        } catch (Exception e) {
            logger.error("{} Rmq4j, SSL configuration for RabbitMQ could not be applied cause got an exception: {} by URL schema: {}",
                    IconType.ERROR.getCode(), e.getMessage(), this.getURLConnSchema(node), e);
            throw new RuntimeException(String.format("SSL configuration for RabbitMQ could not be applied successfully (%s)", this.getURLConnSchema(node)), e);
        }
    }

    /**
     * Creates a {@link CachingConnectionFactory} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createFactory(Rmq4jProperties.Node)} to create a {@link ConnectionFactory}
     * from the given cluster configuration. If the factory creation is successful, it wraps the factory in a
     * {@link CachingConnectionFactory} and returns it as an {@link Optional}.
     * <p>
     * If the {@link ConnectionFactory} could not be created (e.g., due to invalid configuration), it returns
     * an empty {@link Optional}.
     *
     * @param node The cluster configuration used to create the {@link ConnectionFactory}.
     * @return An {@link Optional} containing the {@link CachingConnectionFactory} if the {@link ConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Node node) {
        Optional<ConnectionFactory> factory = this.createFactory(node);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new CachingConnectionFactory(factory.get()));
    }

    /**
     * Creates a {@link RabbitTemplate} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Node)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitTemplate}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param node The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitTemplate} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<RabbitTemplate> dispatch(Rmq4jProperties.Node node) {
        Optional<CachingConnectionFactory> factory = this.createCacheConnFactory(node);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new RabbitTemplate(factory.get()));
    }

    /**
     * Creates a {@link RabbitAdmin} for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method first uses {@link #createCacheConnFactory(Rmq4jProperties.Node)} to create a
     * {@link CachingConnectionFactory} from the given cluster configuration. If the creation of the
     * {@link CachingConnectionFactory} is successful, it wraps the factory in a {@link RabbitAdmin}
     * and returns it as an {@link Optional}.
     * <p>
     * If the {@link CachingConnectionFactory} could not be created (e.g., due to invalid configuration),
     * it returns an empty {@link Optional}.
     *
     * @param node The cluster configuration used to create the {@link CachingConnectionFactory}.
     * @return An {@link Optional} containing the {@link RabbitAdmin} if the {@link CachingConnectionFactory}
     * was created successfully; otherwise, an empty {@link Optional}.
     */
    @SuppressWarnings({"OptionalIsPresent"})
    @Override
    public Optional<RabbitAdmin> createAdm(Rmq4jProperties.Node node) {
        Optional<CachingConnectionFactory> factory = this.createCacheConnFactory(node);
        if (!factory.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new RabbitAdmin(factory.get()));
    }

    /**
     * Generates the connection URL schema for RabbitMQ based on the provided cluster configuration.
     * <p>
     * This method constructs the connection URL schema using the username, password, host, port, and virtual host from
     * the provided {@link Rmq4jProperties.Node}. The URL is formatted as "amqp://username:password@host:port".
     * If a virtual host is specified, it is appended to the URL.
     * <p>
     * If the provided configuration is null, it returns an empty string.
     *
     * @param node The cluster configuration used to build the connection URL.
     * @return A string representing the RabbitMQ connection URL schema based on the provided configuration.
     */
    @Override
    public String getURLConnSchema(Rmq4jProperties.Node node) {
        if (node == null) {
            return "";
        }
        String form = String.format("amqp://%s:%s@%s:%d",
                node.getUsername(), node.getPassword(), node.getHost(), node.getPort());
        if (String4j.isEmpty(node.getVirtualHost())) {
            return form;
        }
        return String.format("%s%s", form, node.getVirtualHost());
    }
}
