package com.elevator.ops.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elevator.cors")
public record CorsProperties(List<String> allowedOrigins) {}

