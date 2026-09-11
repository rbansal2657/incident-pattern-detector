package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic dependency degradation detector evaluating external services, HTTP endpoints, Redis, gRPC, and DB calls.
 */
public class DependencyDegradationRule implements IncidentRule {

    @Override
    public String pattern() {
        return "DEPENDENCY_DEGRADATION";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double depLatency = snapshot.getMetric("dependency.latency_p95");
        Double depErrorRate = snapshot.getMetric("dependency.error_rate");
        Double depTimeoutRate = snapshot.getMetric("dependency.timeout_rate");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (depLatency == null && depErrorRate == null && depTimeoutRate == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        Double depLatencyBaseline = baseline.getMovingAverage("dependency.latency_p95", Duration.ofMinutes(5));
        if (depLatency != null && depLatencyBaseline != null && depLatencyBaseline > 0 && depLatency >= depLatencyBaseline * 3) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("dependency.latency_p95", String.format("%.0fms", depLatency),
                    String.format("%.0fms", depLatencyBaseline), String.format("%.0fms", depLatencyBaseline * 3), "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (depErrorRate != null && depErrorRate > 0.05) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("dependency.error_rate", String.format("%.1f%%", depErrorRate * 100),
                    "0.5%", "5.0%", "%", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (depTimeoutRate != null && depTimeoutRate > 0.03) {
            double contrib = 0.25;
            score += contrib;
            evidence.add(new Evidence("dependency.timeout_rate", String.format("%.1f%%", depTimeoutRate * 100),
                    "0.1%", "3.0%", "%", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.CRITICAL : (score >= 0.70 ? Severity.HIGH : Severity.MEDIUM);

        List<String> probableCauses = List.of(
                "Remote downstream dependency experiencing high load, database bottlenecks, or network degradation.",
                "Mismatched connection timeouts or pool exhaustion on remote host.",
                "Network packet loss or routing issues between services."
        );

        List<String> potentialImpacts = List.of(
                "Upstream request processing latency elevated due to blocking remote calls.",
                "Cascade error propagation returning 502/504 errors to end users."
        );

        List<String> recommendedActions = List.of(
                "Check downstream dependency health metrics and error logs.",
                "Verify network connectivity, DNS resolution, and firewall latencies.",
                "Enable fallback / circuit breaker responses for degraded remote calls."
        );

        List<Correlation> correlations = List.of(
                new Correlation("dependency.latency_p95", "request.latency_p95", Duration.ofMillis(200), 0.95,
                        "Dependency latency increase directly accounts for upstream API latency spike.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Dependency degradation detected: downstream dependency latency and error rates elevated.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "external-dependency"))
                .build();
    }
}
