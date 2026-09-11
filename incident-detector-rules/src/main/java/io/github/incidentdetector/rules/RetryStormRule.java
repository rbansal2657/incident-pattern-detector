package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects retry storms and retry amplification loops triggered by failing downstream dependencies.
 */
public class RetryStormRule implements IncidentRule {

    @Override
    public String pattern() {
        return "RETRY_STORM";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double retryRatio = snapshot.getMetric("http.retry_ratio");
        Double retryRate = snapshot.getMetric("http.retry_rate");
        Double reqRate = snapshot.getMetric("http.request_rate");
        Double depErrorRate = snapshot.getMetric("dependency.error_rate");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (retryRatio == null && retryRate == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        if (retryRatio != null && retryRatio >= 0.20) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("http.retry_ratio", String.format("%.0f%%", retryRatio * 100),
                    "2%", "20%", "%", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        Double retryRateBaseline = baseline.getMovingAverage("http.retry_rate", Duration.ofMinutes(5));
        if (retryRate != null && retryRateBaseline != null && retryRateBaseline > 0 && retryRate >= retryRateBaseline * 4) {
            double contrib = 0.30;
            score += contrib;
            evidence.add(new Evidence("http.retry_rate", String.format("%.1frps", retryRate),
                    String.format("%.1frps", retryRateBaseline), String.format("%.1frps", retryRateBaseline * 4), "rps", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (depErrorRate != null && depErrorRate > 0.10) {
            double contrib = 0.20;
            score += contrib;
            evidence.add(new Evidence("dependency.error_rate", String.format("%.1f%%", depErrorRate * 100),
                    "0.5%", "10.0%", "%", "ELEVATED", Duration.ofSeconds(10), contrib));
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
                "Downstream dependency failures triggering automated client retries.",
                "Lack of exponential backoff and jitter in HTTP/gRPC client configurations.",
                "Cascade amplification multiplying total request volume."
        );

        List<String> potentialImpacts = List.of(
                "Downstream service overwhelm preventing recovery.",
                "Thread starvation and high latency on the calling application."
        );

        List<String> recommendedActions = List.of(
                "Verify client retry settings (enable circuit breakers, exponential backoff, jitter).",
                "Check downstream dependency health and status codes.",
                "Rate-limit incoming request retries."
        );

        List<Correlation> correlations = List.of(
                new Correlation("dependency.error_rate", "http.retry_rate", Duration.ofMillis(300), 0.95,
                        "Dependency error rate spike preceded the sudden escalation in client retry rate.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Retry storm detected: rapid multiplication of client retries correlated with dependency errors.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "http-client"))
                .build();
    }
}
