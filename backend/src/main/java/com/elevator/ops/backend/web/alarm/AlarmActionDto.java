package com.elevator.ops.backend.web.alarm;

import java.time.Instant;

public record AlarmActionDto(
    long id,
    String actionType,
    String note,
    String operatorUserId,
    String operatorName,
    String operatorRole,
    Instant createdAt) {}

