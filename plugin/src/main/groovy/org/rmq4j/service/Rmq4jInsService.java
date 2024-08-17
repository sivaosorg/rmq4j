package org.rmq4j.service;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

public interface Rmq4jInsService {

    void snapIns();

    boolean exists();

    Optional<CachingConnectionFactory> getWorker(String key);

    Optional<RabbitTemplate> getDispatcher(String key);

    void publish(String key, Object message);
}
