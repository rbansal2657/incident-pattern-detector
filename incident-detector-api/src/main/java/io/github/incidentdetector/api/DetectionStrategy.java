package io.github.incidentdetector.api;

/**
 * Detection strategies supported by rules and baseline comparison engines.
 */
public enum DetectionStrategy {
    ABSOLUTE_THRESHOLD,
    PERCENT_CHANGE,
    BASELINE_DEVIATION,
    MULTIPLIER_FROM_BASELINE,
    RATE_CHANGE,
    MOVING_AVERAGE,
    PERCENTILE_DEVIATION
}
