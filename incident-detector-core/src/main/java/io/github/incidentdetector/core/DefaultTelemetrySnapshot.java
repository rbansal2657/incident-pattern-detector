package io.github.incidentdetector.core;

import io.github.incidentdetector.api.MetricValue;
import io.github.incidentdetector.api.TelemetrySnapshot;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Standard thread-safe, immutable implementation of TelemetrySnapshot.
 */
public class DefaultTelemetrySnapshot implements TelemetrySnapshot {

    private final Instant timestamp;
    private final Map<String, MetricValue> metrics;
    private final Map<String, Map<String, Double>> keyDistributions;

    public DefaultTelemetrySnapshot(Instant timestamp, Map<String, MetricValue> metrics, Map<String, Map<String, Double>> keyDistributions) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.metrics = metrics != null ? Collections.unmodifiableMap(new HashMap<>(metrics)) : Collections.emptyMap();
        
        Map<String, Map<String, Double>> distCopy = new HashMap<>();
        if (keyDistributions != null) {
            for (Map.Entry<String, Map<String, Double>> entry : keyDistributions.entrySet()) {
                distCopy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
            }
        }
        this.keyDistributions = Collections.unmodifiableMap(distCopy);
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Double getMetric(String name) {
        MetricValue mv = metrics.get(name);
        return mv != null ? mv.getValue() : null;
    }

    @Override
    public Double getMetric(String name, Map<String, String> tags) {
        MetricValue mv = metrics.get(name);
        if (mv == null) return null;
        if (tags == null || tags.isEmpty()) return mv.getValue();
        return mv.getTags().entrySet().containsAll(tags.entrySet()) ? mv.getValue() : null;
    }

    @Override
    public Map<String, MetricValue> getAllMetrics() {
        return metrics;
    }

    @Override
    public Map<String, Double> getKeyDistribution(String component) {
        return keyDistributions.getOrDefault(component, Collections.emptyMap());
    }

    @Override
    public List<MetricValue> getMetricsMatching(String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        List<MetricValue> result = new ArrayList<>();
        for (Map.Entry<String, MetricValue> entry : metrics.entrySet()) {
            if (pattern.matcher(entry.getKey()).matches()) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private final Map<String, MetricValue> metrics = new HashMap<>();
        private final Map<String, Map<String, Double>> keyDistributions = new HashMap<>();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder putMetric(String name, double value) {
            this.metrics.put(name, new MetricValue(name, value, timestamp));
            return this;
        }

        public Builder putMetric(String name, double value, String unit, Map<String, String> tags) {
            this.metrics.put(name, new MetricValue(name, value, timestamp, unit, tags));
            return this;
        }

        public Builder putMetric(MetricValue metricValue) {
            this.metrics.put(metricValue.getName(), metricValue);
            return this;
        }

        public Builder putKeyDistribution(String component, Map<String, Double> distribution) {
            this.keyDistributions.put(component, new HashMap<>(distribution));
            return this;
        }

        public DefaultTelemetrySnapshot build() {
            return new DefaultTelemetrySnapshot(timestamp, metrics, keyDistributions);
        }
    }
}
