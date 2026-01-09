package com.elevator.ops.backend.web.device;

import java.time.Instant;

public record DeviceStateChangeDto(
    long id, String deviceId, String eventType, String details, Instant occurredAt) {}

