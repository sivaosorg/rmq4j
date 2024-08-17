package org.rmq4j.config;

import org.rmq4j.service.Rmq4jInsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Rmq4jConfig {
    protected final Rmq4jInsService rmq4jInsService;

    @Autowired
    public Rmq4jConfig(Rmq4jInsService rmq4jInsService) {
        this.rmq4jInsService = rmq4jInsService;
    }

    @Bean
    public void snapIns() {
        rmq4jInsService.snapIns();
    }
}
