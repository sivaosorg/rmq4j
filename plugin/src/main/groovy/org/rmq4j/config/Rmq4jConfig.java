package org.rmq4j.config;

import org.rmq4j.service.Rmq4jInsService;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(value = "spring.rmq4j.enabled", havingValue = "true", matchIfMissing = false)
public class Rmq4jConfig {
    protected final Rmq4jInsService rmq4jInsService;

    @Autowired
    public Rmq4jConfig(Rmq4jInsService rmq4jInsService) {
        this.rmq4jInsService = rmq4jInsService;
    }

    @Bean
    public void snapIns() {
        // rmq4jInsService.snapIns();
    }
}
