package io.github.incidentdetector.api;

/**
 * Abstraction for telemetry suppliers (JVM, Redis, HTTP, Kafka, Micrometer, OTel, custom).
 */
public interface TelemetryProvider {

    String name();

    TelemetrySnapshot snapshot();
}
