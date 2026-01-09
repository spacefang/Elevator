package com.elevator.ops.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElevatorBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(ElevatorBackendApplication.class, args);
  }
}
