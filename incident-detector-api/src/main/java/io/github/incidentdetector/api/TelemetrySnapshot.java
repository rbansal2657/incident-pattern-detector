package io.github.incidentdetector.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Immutable point-in-time container of collected telemetry metrics and distribution signals.
 */
public interface TelemetrySnapshot {

    Instant getTimestamp();

    Double getMetric(String name);

    Double getMetric(String name, Map<String, String> tags);

    Map<String, MetricValue> getAllMetrics();

    Map<String, Double> getKeyDistribution(String component);

    List<MetricValue> getMetricsMatching(String pattern);
}
