package io.github.incidentdetector.kafka;

import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;

import java.time.Instant;

/**
 * Telemetry provider for Kafka consumer lag, partition imbalance, and record processing metrics.
 */
public class KafkaTelemetryProvider implements TelemetryProvider {

    private volatile double consumerLag = 0;
    private volatile double lagGrowthRate = 0;
    private volatile double consumerThroughput = 100;
    private volatile double processingLatencyP95 = 15;
    private volatile double partitionImbalance = 0.05;

    @Override
    public String name() {
        return "kafka-telemetry-provider";
    }

    public void updateConsumerLag(double lag, double growthRate, double processLatencyMs) {
        this.consumerLag = lag;
        this.lagGrowthRate = growthRate;
        this.processingLatencyP95 = processLatencyMs;
    }

    public void setPartitionImbalance(double imbalanceRatio) {
        this.partitionImbalance = imbalanceRatio;
    }

    @Override
    public TelemetrySnapshot snapshot() {
        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(Instant.now());
        builder.putMetric("kafka.consumer_lag", consumerLag);
        builder.putMetric("kafka.lag_growth_rate", lagGrowthRate);
        builder.putMetric("kafka.consumer_throughput", consumerThroughput);
        builder.putMetric("kafka.processing_latency_p95", processingLatencyP95);
        builder.putMetric("kafka.partition_imbalance", partitionImbalance);
        return builder.build();
    }
}
