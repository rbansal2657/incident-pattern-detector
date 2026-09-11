package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects Kafka / Message Consumer lag accumulation and processing bottlenecks.
 */
public class ConsumerLagRule implements IncidentRule {

    @Override
    public String pattern() {
        return "CONSUMER_LAG";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double lag = snapshot.getMetric("kafka.consumer_lag");
        Double growthRate = snapshot.getMetric("kafka.lag_growth_rate");
        Double throughput = snapshot.getMetric("kafka.consumer_throughput");
        Double processLatency = snapshot.getMetric("kafka.processing_latency_p95");
        Double partitionImbalance = snapshot.getMetric("kafka.partition_imbalance");

        if (lag == null && growthRate == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        Double lagBaseline = baseline.getMovingAverage("kafka.consumer_lag", Duration.ofMinutes(5));
        if (lag != null && lag > 500) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("kafka.consumer_lag", lag.longValue(),
                    lagBaseline != null ? lagBaseline.longValue() : 50, 500, "records", "ELEVATED", Duration.ofSeconds(10), contrib));
        }

        if (growthRate != null && growthRate > 10.0) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("kafka.lag_growth_rate", String.format("+%.1f rec/s", growthRate),
                    "0 rec/s", "+10 rec/s", "rec/s", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        Double latencyBaseline = baseline.getMovingAverage("kafka.processing_latency_p95", Duration.ofMinutes(5));
        if (processLatency != null && processLatency > 300) {
            double contrib = 0.25;
            score += contrib;
            evidence.add(new Evidence("kafka.processing_latency_p95", String.format("%.0fms", processLatency),
                    latencyBaseline != null ? String.format("%.0fms", latencyBaseline) : "15ms", "300ms", "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (partitionImbalance != null && partitionImbalance > 0.40) {
            double contrib = 0.15;
            score += contrib;
            evidence.add(new Evidence("kafka.partition_imbalance", String.format("%.1f%%", partitionImbalance * 100),
                    "5%", "40%", "%", "ELEVATED", Duration.ofSeconds(10), contrib));
        }

        if (score < 0.50 || evidence.size() < 2) {
            return DetectionResult.notTriggered();
        }

        Severity severity = score >= 0.85 ? Severity.CRITICAL : Severity.HIGH;

        List<String> probableCauses = List.of(
                "Consumer record processing rate is slower than producer message publishing rate.",
                "Partition key imbalance causing single consumer thread saturation.",
                "Slow database/downstream network calls inside consumer record listener loop."
        );

        List<String> potentialImpacts = List.of(
                "Stale message processing delays affecting real-time business pipelines.",
                "Consumer group rebalancing risk if poll timeout (max.poll.interval.ms) is exceeded."
        );

        List<String> recommendedActions = List.of(
                "Scale out consumer group instances up to total partition count.",
                "Batch downstream DB writes inside record listener.",
                "Check partition key hashing strategy to ensure uniform workload distribution."
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Consumer lag accumulation detected: message lag growing steadily over time.")
                .evidence(evidence)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "kafka-consumer"))
                .build();
    }
}
