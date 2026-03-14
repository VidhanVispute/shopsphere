package com.shopsphere.search.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue.search}")
    private String searchQueue;

    @Bean
    public TopicExchange shopSphereExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue searchQueue() {
        return QueueBuilder.durable(searchQueue).build();
    }

    @Bean
    public Binding bindProductEvents(Queue searchQueue, TopicExchange shopSphereExchange) {
        return BindingBuilder.bind(searchQueue)
                .to(shopSphereExchange).with("product.*");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}