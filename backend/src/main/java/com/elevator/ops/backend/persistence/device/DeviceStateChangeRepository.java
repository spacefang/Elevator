package com.elevator.ops.backend.persistence.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceStateChangeRepository extends JpaRepository<DeviceStateChange, Long> {
  Page<DeviceStateChange> findByDeviceIdOrderByOccurredAtDesc(String deviceId, Pageable pageable);
}
