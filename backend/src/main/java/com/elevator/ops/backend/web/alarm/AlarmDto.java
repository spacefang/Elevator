package com.elevator.ops.backend.web.alarm;

import java.time.Instant;

public record AlarmDto(
    long id,
    String deviceId,
    String level,
    String type,
    String description,
    Instant occurredAt,
    Instant createdAt) {}
