package com.shopsphere.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * @EnableConfigServer is the only thing that makes this different
 * from a plain Spring Boot app. It activates all the Config Server
 * machinery — the /config endpoint, the Git backend, the environment
 * repository — with that single annotation.
 *
 * That's intentional Spring Cloud design: one annotation, full feature.
 * Under the hood it imports EnvironmentRepositoryConfiguration,
 * ConfigServerMvcConfiguration, and several other @Configuration classes.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}