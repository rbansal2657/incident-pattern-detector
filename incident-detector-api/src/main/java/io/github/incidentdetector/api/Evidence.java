package io.github.incidentdetector.api;

import java.time.Duration;

/**
 * Represents an individual telemetry signal contribution supporting a detected pattern.
 */
public class Evidence {

    private final String metric;
    private final Object currentValue;
    private final Object baselineValue;
    private final Object threshold;
    private final String unit;
    private final String direction;
    private final Duration duration;
    private final double contribution;

    public Evidence(String metric, Object currentValue, Object baselineValue, Object threshold,
                    String unit, String direction, Duration duration, double contribution) {
        this.metric = metric;
        this.currentValue = currentValue;
        this.baselineValue = baselineValue;
        this.threshold = threshold;
        this.unit = unit;
        this.direction = direction;
        this.duration = duration;
        this.contribution = contribution;
    }

    public String getMetric() {
        return metric;
    }

    public Object getCurrentValue() {
        return currentValue;
    }

    public Object getBaselineValue() {
        return baselineValue;
    }

    public Object getThreshold() {
        return threshold;
    }

    public String getUnit() {
        return unit;
    }

    public String getDirection() {
        return direction;
    }

    public Duration getDuration() {
        return duration;
    }

    public double getContribution() {
        return contribution;
    }

    @Override
    public String toString() {
        return String.format("%s = %s%s (baseline: %s, threshold: %s, direction: %s, contribution: %.0f%%)",
                metric, currentValue, unit != null ? unit : "",
                baselineValue != null ? baselineValue : "N/A",
                threshold != null ? threshold : "N/A",
                direction != null ? direction : "UNKNOWN",
                contribution * 100);
    }
}
