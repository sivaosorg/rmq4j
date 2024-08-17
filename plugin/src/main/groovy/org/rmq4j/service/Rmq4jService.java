package org.rmq4j.service;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.config.props.Rmq4jProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.Optional;

public interface Rmq4jService {

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
    boolean isEnabled();

    boolean isDebugging();

    Map<String, Rmq4jProperties.Node> getClusters();

    Optional<Rmq4jProperties.Node> getNode(String key);

    Optional<Rmq4jProperties.Node> getNodeActivated(String key);

    Optional<ConnectionFactory> createFactory(Rmq4jProperties.Node node);

    Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Node node);

    Optional<RabbitTemplate> dispatch(Rmq4jProperties.Node node);

    Optional<RabbitTemplate> dispatch(CachingConnectionFactory factory);

    Optional<RabbitAdmin> createAdm(Rmq4jProperties.Node node);

    String getURLConnSchema(Rmq4jProperties.Node node);
}
