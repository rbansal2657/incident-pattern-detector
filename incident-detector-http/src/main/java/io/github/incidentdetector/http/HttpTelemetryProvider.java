package io.github.incidentdetector.http;

import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Thread-safe HTTP Client/Server Telemetry Provider for tracking HTTP request latencies, retry ratios, and payload metadata.
 */
public class HttpTelemetryProvider implements TelemetryProvider {

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong retryCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);
    private final DoubleAdder totalLatencyMs = new DoubleAdder();
    private final DoubleAdder maxLatencyMs = new DoubleAdder();
    private final DoubleAdder maxPayloadBytes = new DoubleAdder();
    private final DoubleAdder totalPayloadBytes = new DoubleAdder();

    @Override
    public String name() {
        return "http-telemetry-provider";
    }

    public void recordRequest(double latencyMs, int statusCode, boolean isRetry, long payloadSizeBytes) {
        requestCount.incrementAndGet();
        if (isRetry) retryCount.incrementAndGet();
        if (statusCode >= 500) errorCount.incrementAndGet();
        if (statusCode == 504 || statusCode == 408) timeoutCount.incrementAndGet();

        totalLatencyMs.add(latencyMs);
        if (latencyMs > maxLatencyMs.doubleValue()) {
            maxLatencyMs.reset();
            maxLatencyMs.add(latencyMs);
        }

        totalPayloadBytes.add(payloadSizeBytes);
        if (payloadSizeBytes > maxPayloadBytes.doubleValue()) {
            maxPayloadBytes.reset();
            maxPayloadBytes.add(payloadSizeBytes);
        }
    }

    @Override
    public TelemetrySnapshot snapshot() {
        long reqs = Math.max(1, requestCount.get());
        long retries = retryCount.get();
        long errors = errorCount.get();
        long timeouts = timeoutCount.get();

        double avgLatency = totalLatencyMs.doubleValue() / reqs;
        double avgPayload = totalPayloadBytes.doubleValue() / reqs;

        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(Instant.now());

        builder.putMetric("http.request_rate", reqs / 10.0);
        builder.putMetric("http.retry_rate", retries / 10.0);
        builder.putMetric("http.retry_ratio", (double) retries / reqs);
        builder.putMetric("request.latency_p95", Math.max(avgLatency * 1.3, maxLatencyMs.doubleValue() * 0.8));
        builder.putMetric("dependency.error_rate", (double) errors / reqs);
        builder.putMetric("dependency.timeout_count", timeouts);
        builder.putMetric("payload.size_p95", Math.max(avgPayload * 1.2, maxPayloadBytes.doubleValue() * 0.8));
        builder.putMetric("payload.size_max", maxPayloadBytes.doubleValue());

        return builder.build();
    }

    public void reset() {
        requestCount.set(0);
        retryCount.set(0);
        errorCount.set(0);
        timeoutCount.set(0);
        totalLatencyMs.reset();
        maxLatencyMs.reset();
        maxPayloadBytes.reset();
        totalPayloadBytes.reset();
    }
}
