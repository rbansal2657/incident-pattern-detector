package io.github.incidentdetector.api;

/**
 * Generic rule abstraction for evaluating a specific failure pattern.
 */
public interface IncidentRule {

    String pattern();

    default boolean isEnabled() {
        return true;
    }

    DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline);
}
