package com.elevator.ops.backend.sim;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elevator.simulator")
public record SimulatorProperties(
    boolean enabled,
    int deviceCount,
    int publishIntervalMs,
    int minFloor,
    int maxFloor,
    double offlineChance,
    int offlineSecondsMin,
    int offlineSecondsMax,
    double trappedChance) {}

