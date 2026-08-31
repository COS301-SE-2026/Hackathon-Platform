package com.hackathon.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Hackathon Platform Spring Boot application. This class bootstraps the
 * application using Spring Boot.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HackathonPlatformApplication {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(HackathonPlatformApplication.class, args);
  }
}
