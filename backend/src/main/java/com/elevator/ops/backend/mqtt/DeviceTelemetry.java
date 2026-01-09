package com.elevator.ops.backend.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceTelemetry(
    @JsonProperty("device_id") String deviceId,
    @JsonProperty("timestamp") long timestampMs,
    @JsonProperty("floor") int floor,
    @JsonProperty("direction") String direction,
    @JsonProperty("door_status") String doorStatus,
    @JsonProperty("speed") double speed,
    @JsonProperty("load") double load,
    @JsonProperty("temperature") double temperature,
    @JsonProperty("vibration") double vibration,
    @JsonProperty("power") double power) {}

