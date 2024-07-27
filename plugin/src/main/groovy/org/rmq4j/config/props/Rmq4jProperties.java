package org.rmq4j.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@ConfigurationProperties(prefix = "spring.rmq4j")
public class Rmq4jProperties implements Serializable {
    public Rmq4jProperties() {
        super();
    }
}
