package com.thomsonreuters.handler;

import com.google.inject.Singleton;
import com.netflix.karyon.health.HealthCheckHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@Singleton
public class BasicHealthCheck implements HealthCheckHandler {
    private static final Logger log = LoggerFactory.getLogger(BasicHealthCheck.class);

    @PostConstruct
    public void init() {
        log.info("Health check initialized.");
    }

    @Override
    public int getStatus() {
        log.info("Health check called.");
        return 200;
    }
}
