package com.elevator.ops.backend.persistence.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "device_state_changes")
public class DeviceStateChange {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", nullable = false, length = 64)
  private String deviceId;

  @Column(name = "event_type", nullable = false, length = 32)
  private String eventType;

  @Column(name = "details", columnDefinition = "text")
  private String details;

  @CreationTimestamp
  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  public static DeviceStateChange of(String deviceId, String eventType, String details) {
    DeviceStateChange change = new DeviceStateChange();
    change.setDeviceId(deviceId);
    change.setEventType(eventType);
    change.setDetails(details);
    return change;
  }

  public Long getId() {
    return id;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
