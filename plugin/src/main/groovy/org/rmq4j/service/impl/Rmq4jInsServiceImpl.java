package org.rmq4j.service.impl;

import org.rmq4j.service.Rmq4jInsService;
import org.rmq4j.service.Rmq4jService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings({"FieldCanBeLocal", "DuplicatedCode"})
@Service
public class Rmq4jInsServiceImpl implements Rmq4jInsService {
    protected static final Logger logger = LoggerFactory.getLogger(Rmq4jInsServiceImpl.class);

    protected final Rmq4jService rmq4jService;

    @Autowired
    public Rmq4jInsServiceImpl(Rmq4jService rmq4jService) {
        this.rmq4jService = rmq4jService;
    }
}
