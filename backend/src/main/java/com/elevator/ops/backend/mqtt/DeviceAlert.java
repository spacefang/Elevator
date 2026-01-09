package com.elevator.ops.backend.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceAlert(
    @JsonProperty("device_id") String deviceId,
    @JsonProperty("alert_id") String alertId,
    @JsonProperty("alert_level") String alertLevel,
    @JsonProperty("alert_type") String alertType,
    @JsonProperty("alert_desc") String alertDesc,
    @JsonProperty("timestamp") long timestampMs,
    @JsonProperty("location") String location,
    @JsonProperty("trapped_count") Integer trappedCount) {}

