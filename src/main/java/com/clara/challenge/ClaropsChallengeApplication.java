package com.clara.challenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Clarops Engineering Challenge application.
 *
 * <p>Bootstraps Spring Boot with auto-configuration, component scanning, and an embedded
 * Tomcat server. The application exposes REST endpoints for receiving distributed events
 * and querying trace status, backed by PostgreSQL.</p>
 *
 * <p>Active profiles and database connectivity are configured via {@code application.yaml}.
 * Docker Compose support is enabled for automatic PostgreSQL lifecycle management.</p>
 */
@SpringBootApplication
public class ClaropsChallengeApplication {

  /**
   * Launches the Spring Boot application.
   *
   * @param args command-line arguments passed to the JVM
   */
  public static void main(String[] args) {

    SpringApplication.run(ClaropsChallengeApplication.class, args);
  }
}
