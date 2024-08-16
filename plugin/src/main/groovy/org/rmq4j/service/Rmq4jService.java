package org.rmq4j.service;

import com.rabbitmq.client.ConnectionFactory;
import org.rmq4j.config.props.Rmq4jProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

public interface Rmq4jService {

    boolean isEnabled();

    boolean isDebugging();

    Optional<Rmq4jProperties.Node> getCluster(String key);

    Optional<Rmq4jProperties.Node> getClusterActivated(String key);

    Optional<ConnectionFactory> createFactory(Rmq4jProperties.Node node);

    Optional<CachingConnectionFactory> createCacheConnFactory(Rmq4jProperties.Node node);

    Optional<RabbitTemplate> dispatch(Rmq4jProperties.Node node);

    String getURLConnSchema(Rmq4jProperties.Node node);
}
