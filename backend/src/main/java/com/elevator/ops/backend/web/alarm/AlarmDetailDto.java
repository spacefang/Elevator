package com.elevator.ops.backend.web.alarm;

import java.time.Instant;
import java.util.List;

public record AlarmDetailDto(
    long id,
    String deviceId,
    String location,
    String level,
    String status,
    String type,
    String description,
    Instant occurredAt,
    Instant createdAt,
    List<AlarmActionDto> actions) {}

