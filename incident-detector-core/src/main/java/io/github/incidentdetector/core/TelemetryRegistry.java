package io.github.incidentdetector.core;

import io.github.incidentdetector.api.MetricValue;
import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe Telemetry Registry combining metrics from registered TelemetryProviders and direct application inputs.
 */
public class TelemetryRegistry {

    private final List<TelemetryProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<String, MetricValue> customMetrics = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> customKeyDistributions = new ConcurrentHashMap<>();

    public TelemetryRegistry(List<TelemetryProvider> providers) {
        if (providers != null) {
            this.providers.addAll(providers);
        }
    }

    public TelemetryRegistry() {
    }

    public void registerProvider(TelemetryProvider provider) {
        if (provider != null) {
            providers.add(provider);
        }
    }

    public void recordMetric(String name, double value) {
        recordMetric(name, value, "", Collections.emptyMap());
    }

    public void recordMetric(String name, double value, String unit, Map<String, String> tags) {
        customMetrics.put(name, new MetricValue(name, value, Instant.now(), unit, tags));
    }

    public void recordKeyDistribution(String component, Map<String, Double> distribution) {
        customKeyDistributions.put(component, new HashMap<>(distribution));
    }

    public TelemetrySnapshot captureCombinedSnapshot() {
        Instant now = Instant.now();
        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(now);

        // Direct custom metrics
        for (MetricValue mv : customMetrics.values()) {
            builder.putMetric(mv);
        }
        for (Map.Entry<String, Map<String, Double>> entry : customKeyDistributions.entrySet()) {
            builder.putKeyDistribution(entry.getKey(), entry.getValue());
        }

        // Provider metrics
        for (TelemetryProvider provider : providers) {
            try {
                TelemetrySnapshot snap = provider.snapshot();
                if (snap != null) {
                    if (snap.getAllMetrics() != null) {
                        for (MetricValue mv : snap.getAllMetrics().values()) {
                            builder.putMetric(mv);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return builder.build();
    }

    public List<TelemetryProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }
}
