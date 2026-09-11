package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;
import io.github.incidentdetector.core.SlidingWindowBaselineEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AllRulesTest {

    private final SlidingWindowBaselineEngine baseline = new SlidingWindowBaselineEngine();

    @Test
    public void testPoolSaturationRule() {
        PoolSaturationRule rule = new PoolSaturationRule();
        
        // Normal
        DetectionResult normal = rule.evaluate(DefaultTelemetrySnapshot.builder().putMetric("pool.utilization", 0.30).build(), baseline);
        assertFalse(normal.isTriggered());

        // Incident
        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("pool.utilization", 0.98)
                .putMetric("pool.pending_acquisitions", 25)
                .putMetric("pool.acquire_latency_p95", 750)
                .putMetric("request.latency_p95", 900)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("POOL_SATURATION", rule.pattern());
        assertTrue(incident.getConfidence() >= 0.80);
        assertTrue(incident.getEvidence().size() >= 2);
    }

    @Test
    public void testRetryStormRule() {
        RetryStormRule rule = new RetryStormRule();
        
        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("http.retry_ratio", 0.35)
                .putMetric("dependency.error_rate", 0.25)
                .putMetric("request.latency_p95", 600)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("RETRY_STORM", rule.pattern());
    }

    @Test
    public void testTimeoutCascadeRule() {
        TimeoutCascadeRule rule = new TimeoutCascadeRule();

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("dependency.timeout_count", 15)
                .putMetric("upstream.timeout_count", 8)
                .putMetric("request.latency_p95", 1200)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("TIMEOUT_CASCADE", rule.pattern());
    }

    @Test
    public void testHotKeyRuleAndMasking() {
        HotKeyRule rule = new HotKeyRule(0.35);

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putKeyDistribution("redis", Map.of("user_secret_token_12345", 80.0, "key2", 10.0, "key3", 10.0))
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("HOT_KEY", rule.pattern());
        
        // Ensure sensitive raw key is NOT exposed in dimensions or evidence
        String keyHash = incident.getDimensions().get("key_hash");
        assertNotNull(keyHash);
        assertTrue(keyHash.startsWith("sha256:"));
        assertFalse(keyHash.contains("user_secret_token_12345"));
    }

    @Test
    public void testCacheThrashingRule() {
        CacheThrashingRule rule = new CacheThrashingRule();

        baseline.recordSnapshot(DefaultTelemetrySnapshot.builder()
                .putMetric("cache.eviction_rate", 10)
                .putMetric("backend.request_rate", 50)
                .build());

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("cache.hit_ratio", 0.20)
                .putMetric("cache.eviction_rate", 200)
                .putMetric("backend.request_rate", 300)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("CACHE_THRASHING", rule.pattern());
    }

    @Test
    public void testThreadPoolStarvationRuleMultiSignal() {
        ThreadPoolStarvationRule rule = new ThreadPoolStarvationRule();

        // Single signal (active == max) should NOT trigger
        DetectionResult singleSignal = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("executor.active_threads", 20)
                .putMetric("executor.max_threads", 20)
                .build(), baseline);
        assertFalse(singleSignal.isTriggered());

        // Multi-signal SHOULD trigger
        DetectionResult multiSignal = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("executor.active_threads", 20)
                .putMetric("executor.max_threads", 20)
                .putMetric("executor.queue_depth", 150)
                .putMetric("executor.task_wait_time_p95", 450)
                .build(), baseline);

        assertTrue(multiSignal.isTriggered());
        assertEquals("THREAD_POOL_STARVATION", rule.pattern());
    }

    @Test
    public void testConsumerLagRule() {
        ConsumerLagRule rule = new ConsumerLagRule();

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("kafka.consumer_lag", 1500)
                .putMetric("kafka.lag_growth_rate", 50)
                .putMetric("kafka.processing_latency_p95", 450)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("CONSUMER_LAG", rule.pattern());
    }

    @Test
    public void testLargePayloadRule() {
        LargePayloadRule rule = new LargePayloadRule(1024 * 1024); // 1MB threshold

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("payload.size_p95", 5 * 1024 * 1024)
                .putMetric("payload.serialization_latency_p95", 250)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("LARGE_PAYLOAD", rule.pattern());
    }

    @Test
    public void testGcLatencyRule() {
        GcLatencyRule rule = new GcLatencyRule();

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("jvm.gc.pause_duration", 650)
                .putMetric("jvm.gc.full_gc_count", 2)
                .putMetric("jvm.memory.heap_utilization", 0.92)
                .putMetric("request.latency_p95", 800)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("GC_LATENCY", rule.pattern());
    }

    @Test
    public void testDependencyDegradationRule() {
        DependencyDegradationRule rule = new DependencyDegradationRule();

        baseline.recordSnapshot(DefaultTelemetrySnapshot.builder().putMetric("dependency.latency_p95", 50).build());

        DetectionResult incident = rule.evaluate(DefaultTelemetrySnapshot.builder()
                .putMetric("dependency.latency_p95", 450)
                .putMetric("dependency.error_rate", 0.15)
                .build(), baseline);

        assertTrue(incident.isTriggered());
        assertEquals("DEPENDENCY_DEGRADATION", rule.pattern());
    }
}
