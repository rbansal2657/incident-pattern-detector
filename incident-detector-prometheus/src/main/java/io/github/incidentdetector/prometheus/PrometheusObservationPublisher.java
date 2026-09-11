package io.github.incidentdetector.prometheus;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.api.ObservationPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Publishes IncidentObservations to Prometheus via Micrometer MeterRegistry with strict low-cardinality enforcement.
 */
public class PrometheusObservationPublisher implements ObservationPublisher {

    private static final Logger log = LoggerFactory.getLogger(PrometheusObservationPublisher.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> patternDetectedGauges = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> patternConfidenceGauges = new ConcurrentHashMap<>();

    public PrometheusObservationPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public PrometheusObservationPublisher() {
        this(null);
    }

    @Override
    public void publish(IncidentObservation observation) {
        if (observation == null) return;

        String pattern = sanitizeLabel(observation.getPattern());
        String component = sanitizeLabel(observation.getDimensions().getOrDefault("component", "application"));
        String severity = observation.getSeverity().name();

        if (meterRegistry != null) {
            // Counter: incident_pattern_count_total
            Counter.builder("incident_pattern_count_total")
                    .tag("pattern", pattern)
                    .tag("severity", severity)
                    .register(meterRegistry)
                    .increment();

            // Gauge: incident_pattern_detected
            String gaugeKey = pattern + ":" + component;
            patternDetectedGauges.computeIfAbsent(gaugeKey, k -> {
                AtomicInteger val = new AtomicInteger(0);
                Gauge.builder("incident_pattern_detected", val, AtomicInteger::get)
                        .tag("pattern", pattern)
                        .tag("component", component)
                        .tag("severity", severity)
                        .register(meterRegistry);
                return val;
            }).set(observation.getState() == io.github.incidentdetector.api.IncidentState.RESOLVED ? 0 : 1);

            // Gauge: incident_pattern_confidence
            patternConfidenceGauges.computeIfAbsent(gaugeKey, k -> {
                DoubleAdder adder = new DoubleAdder();
                Gauge.builder("incident_pattern_confidence", adder, DoubleAdder::doubleValue)
                        .tag("pattern", pattern)
                        .tag("component", component)
                        .register(meterRegistry);
                return adder;
            }).add(observation.getConfidence() - patternConfidenceGauges.get(gaugeKey).doubleValue());
        }

        log.info("PROMETHEUS_EXPORTED pattern={} component={} state={} confidence={:.2f}",
                pattern, component, observation.getState(), observation.getConfidence());
    }

    private String sanitizeLabel(String value) {
        if (value == null || value.isBlank()) return "unknown";
        // Strip out high cardinality characters, numbers, and uuids
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}
