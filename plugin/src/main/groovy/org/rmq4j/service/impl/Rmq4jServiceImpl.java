package org.rmq4j.service.impl;

import org.rmq4j.config.props.Rmq4jProperties;
import org.rmq4j.service.Rmq4jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings({"FieldCanBeLocal", "DuplicatedCode"})
@Service
public class Rmq4jServiceImpl implements Rmq4jService {
    protected static final Logger logger = LoggerFactory.getLogger(Rmq4jServiceImpl.class);

    protected final Rmq4jProperties properties;

    @Autowired
    public Rmq4jServiceImpl(Rmq4jProperties properties) {
        this.properties = properties;
    }
}
