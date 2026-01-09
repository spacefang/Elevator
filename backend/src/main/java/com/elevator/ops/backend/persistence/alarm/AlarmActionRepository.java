package com.elevator.ops.backend.persistence.alarm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmActionRepository extends JpaRepository<AlarmAction, Long> {
  List<AlarmAction> findByAlarmIdOrderByCreatedAtDesc(long alarmId);
}

