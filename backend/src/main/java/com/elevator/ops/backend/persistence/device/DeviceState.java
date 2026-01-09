package com.elevator.ops.backend.persistence.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "device_states")
public class DeviceState {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", nullable = false, unique = true, length = 64)
  private String deviceId;

  @Column(name = "online", nullable = false)
  private boolean online;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  @Column(name = "floor")
  private Integer floor;

  @Column(name = "direction", length = 16)
  private String direction;

  @Column(name = "door_status", length = 16)
  private String doorStatus;

  @Column(name = "speed")
  private Double speed;

  @Column(name = "load_value")
  private Double load;

  @Column(name = "temperature")
  private Double temperature;

  @Column(name = "vibration")
  private Double vibration;

  @Column(name = "power")
  private Double power;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public Integer getFloor() {
    return floor;
  }

  public void setFloor(Integer floor) {
    this.floor = floor;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getDoorStatus() {
    return doorStatus;
  }

  public void setDoorStatus(String doorStatus) {
    this.doorStatus = doorStatus;
  }

  public Double getSpeed() {
    return speed;
  }

  public void setSpeed(Double speed) {
    this.speed = speed;
  }

  public Double getLoad() {
    return load;
  }

  public void setLoad(Double load) {
    this.load = load;
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Double getVibration() {
    return vibration;
  }

  public void setVibration(Double vibration) {
    this.vibration = vibration;
  }

  public Double getPower() {
    return power;
  }

  public void setPower(Double power) {
    this.power = power;
  }
}

