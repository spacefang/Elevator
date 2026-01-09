package com.elevator.ops.backend.persistence.alarm;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {
  Page<Alarm> findAllByOrderByOccurredAtDesc(Pageable pageable);

  Page<Alarm> findByDeviceIdInOrderByOccurredAtDesc(List<String> deviceIds, Pageable pageable);
}
