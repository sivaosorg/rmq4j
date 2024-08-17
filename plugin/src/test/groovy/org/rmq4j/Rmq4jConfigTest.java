package org.rmq4j;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rmq4j.config.Rmq4jConfig;
import org.rmq4j.service.Rmq4jInsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class Rmq4jConfigTest {

    @Autowired
    private Rmq4jConfig rmq4jConfig;

    @Autowired
    private Rmq4jInsService rmq4jInsService;

    @Test
    void testSnapIns() {
        rmq4jConfig.snapIns();
        verify(rmq4jInsService, times(1)).snapIns();
    }

    @Configuration
    static class TestConfig {
        @Bean
        public Rmq4jInsService rmq4jInsService() {
            return Mockito.mock(Rmq4jInsService.class);
        }

        @Bean
        public Rmq4jConfig rmq4jConfig(Rmq4jInsService rmq4jInsService) {
            return new Rmq4jConfig(rmq4jInsService);
        }
    }
}