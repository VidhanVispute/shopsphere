package com.shopsphere.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * @EnableEurekaServer activates the Eureka registry.
 * Exactly like @EnableConfigServer — one annotation, full feature.
 *
 * This service does NOT register itself with another Eureka server.
 * It IS the Eureka server. We configure it to not try to register
 * with itself in application.yml (register-with-eureka: false).
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}