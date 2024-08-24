package org.rmq4j.service;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.config.props.Rmq4jProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Rmq4jService {

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    boolean isEnabled();

    boolean isDebugging();

    boolean isAvailableConfigs();

    Map<String, Rmq4jProperties.Connection> getConnections();

    Optional<Rmq4jProperties.Connection> getConn(String key);

    Optional<Rmq4jProperties.Connection> getConnActivated(String key);

    Map<String, Rmq4jProperties.Connection> getConnectionsActivated();

    List<Rmq4jProperties.Config> getConfigsActivated();

    Optional<ConnectionFactory> createFactory(Rmq4jProperties.Connection connection);

    Optional<ConnectionFactory> createFactory(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback);

    Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Connection connection);

    Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback);

    Optional<RabbitTemplate> dispatch(Rmq4jProperties.Connection connection);

    Optional<RabbitTemplate> dispatch(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback);

    Optional<RabbitTemplate> dispatch(CachingConnectionFactory factory);

    Optional<RabbitTemplate> dispatch(CachingConnectionFactory factory, Rmq4jWrapCallback callback);

    Optional<RabbitAdmin> createAdm(Rmq4jProperties.Connection connection);

    Optional<RabbitAdmin> createAdm(Rmq4jProperties.Connection connection, Rmq4jWrapCallback callback);

    String getURLConnSchema(Rmq4jProperties.Connection connection);
}
