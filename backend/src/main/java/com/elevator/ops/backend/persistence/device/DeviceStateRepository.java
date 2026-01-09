package com.elevator.ops.backend.persistence.device;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceStateRepository extends JpaRepository<DeviceState, Long> {
  Optional<DeviceState> findByDeviceId(String deviceId);

  List<DeviceState> findByOnlineIsTrueAndLastSeenAtBefore(Instant threshold);

  Page<DeviceState> findByDeviceIdIn(List<String> deviceIds, Pageable pageable);
}
