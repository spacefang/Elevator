package com.elevator.ops.backend.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elevator.mqtt")
public record MqttProperties(
    boolean enabled,
    String brokerUrl,
    String username,
    String password,
    String clientId,
    String topicData,
    String topicAlert) {}

