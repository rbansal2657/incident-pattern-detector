package io.github.incidentdetector.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Value object wrapping a single telemetry metric measurement point.
 */
public class MetricValue {

    private final String name;
    private final double value;
    private final Instant timestamp;
    private final String unit;
    private final Map<String, String> tags;

    public MetricValue(String name, double value, Instant timestamp, String unit, Map<String, String> tags) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.unit = unit;
        this.tags = tags != null ? Map.copyOf(tags) : Collections.emptyMap();
    }

    public MetricValue(String name, double value, Instant timestamp) {
        this(name, value, timestamp, "", Collections.emptyMap());
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUnit() {
        return unit;
    }

    public Map<String, String> getTags() {
        return tags;
    }
}
