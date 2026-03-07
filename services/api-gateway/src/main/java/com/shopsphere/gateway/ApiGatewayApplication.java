package com.shopsphere.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * No special annotation needed beyond @SpringBootApplication.
 * Spring Cloud Gateway auto-configures when it's on the classpath.
 *
 * Notice: No @EnableEurekaClient either.
 * Since Spring Cloud 2020, Eureka client auto-registers
 * when spring-cloud-starter-netflix-eureka-client is on the classpath.
 * The explicit annotation is no longer needed.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}