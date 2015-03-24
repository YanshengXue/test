package com.thomsonreuters.handler;

import javax.annotation.PostConstruct;

import netflix.karyon.health.HealthCheckHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class HealthCheck implements HealthCheckHandler {
    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

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
