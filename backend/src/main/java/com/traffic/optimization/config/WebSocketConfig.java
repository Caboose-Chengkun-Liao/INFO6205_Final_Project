package com.traffic.optimization.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration - enables real-time data push
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple message broker for pushing messages to clients
        config.enableSimpleBroker("/topic");

        // Set the application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
