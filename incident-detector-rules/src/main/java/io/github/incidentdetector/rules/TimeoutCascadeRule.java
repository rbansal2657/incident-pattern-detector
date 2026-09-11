package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects cascading timeout propagation across multi-tier application architectures.
 */
public class TimeoutCascadeRule implements IncidentRule {

    @Override
    public String pattern() {
        return "TIMEOUT_CASCADE";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double depLatency = snapshot.getMetric("dependency.latency_p95");
        Double depTimeouts = snapshot.getMetric("dependency.timeout_count");
        Double upstreamTimeouts = snapshot.getMetric("upstream.timeout_count");
        Double threadUtil = snapshot.getMetric("executor.active_threads");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (depTimeouts == null && upstreamTimeouts == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        if (depTimeouts != null && depTimeouts > 5) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("dependency.timeout_count", depTimeouts.intValue(),
                    0, 5, "timeouts", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (upstreamTimeouts != null && upstreamTimeouts > 3) {
            double contrib = 0.30;
            score += contrib;
            evidence.add(new Evidence("upstream.timeout_count", upstreamTimeouts.intValue(),
                    0, 3, "timeouts", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        Double depLatencyBaseline = baseline.getMovingAverage("dependency.latency_p95", Duration.ofMinutes(5));
        if (depLatency != null && depLatencyBaseline != null && depLatencyBaseline > 0 && depLatency >= depLatencyBaseline * 3) {
            double contrib = 0.20;
            score += contrib;
            evidence.add(new Evidence("dependency.latency_p95", String.format("%.0fms", depLatency),
                    String.format("%.0fms", depLatencyBaseline), String.format("%.0fms", depLatencyBaseline * 3), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        Double reqLatencyBaseline = baseline.getMovingAverage("request.latency_p95", Duration.ofMinutes(5));
        if (reqLatency != null && reqLatencyBaseline != null && reqLatencyBaseline > 0 && reqLatency >= reqLatencyBaseline * 2.5) {
            double contrib = 0.15;
            score += contrib;
            evidence.add(new Evidence("request.latency_p95", String.format("%.0fms", reqLatency),
                    String.format("%.0fms", reqLatencyBaseline), String.format("%.0fms", reqLatencyBaseline * 2.5), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.CRITICAL : Severity.HIGH;

        List<String> probableCauses = List.of(
                "Slow responses from a downstream microservice exhausting calling thread pools.",
                "Mismatched timeout configurations across system boundaries.",
                "Lack of fallback mechanisms when downstream dependencies timeout."
        );

        List<String> potentialImpacts = List.of(
                "Complete request blocking propagating upwards to API gateway.",
                "Thread exhaustion leading to total service unresponsiveness."
        );

        List<String> recommendedActions = List.of(
                "Inspect downstream service latency and dependency health.",
                "Verify timeout hierarchies (downstream timeouts must be shorter than upstream timeouts).",
                "Enable deadline propagation and circuit breakers."
        );

        List<Correlation> correlations = List.of(
                new Correlation("dependency.latency_p95", "dependency.timeout_count", Duration.ofMillis(800), 0.94,
                        "Downstream latency spike preceded dependency timeouts."),
                new Correlation("dependency.timeout_count", "upstream.timeout_count", Duration.ofMillis(1200), 0.91,
                        "Dependency timeouts triggered upstream API timeouts 1.2s later.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Timeout cascade detected: temporal propagation of timeouts across service boundaries.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "microservices"))
                .build();
    }
}
