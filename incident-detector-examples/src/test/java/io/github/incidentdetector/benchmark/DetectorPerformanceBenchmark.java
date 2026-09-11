package io.github.incidentdetector.benchmark;

import io.github.incidentdetector.api.DetectionResult;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;
import io.github.incidentdetector.core.SlidingWindowBaselineEngine;
import io.github.incidentdetector.rules.PoolSaturationRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DetectorPerformanceBenchmark {

    @Test
    public void benchmarkRuleEvaluationOverhead() {
        PoolSaturationRule rule = new PoolSaturationRule();
        SlidingWindowBaselineEngine baseline = new SlidingWindowBaselineEngine();

        DefaultTelemetrySnapshot snapshot = DefaultTelemetrySnapshot.builder()
                .putMetric("pool.utilization", 0.98)
                .putMetric("pool.pending_acquisitions", 43)
                .putMetric("pool.acquire_latency_p95", 740)
                .putMetric("request.latency_p95", 920)
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            rule.evaluate(snapshot, baseline);
        }

        // Benchmark loop
        int iterations = 100_000;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DetectionResult result = rule.evaluate(snapshot, baseline);
            assertTrue(result.isTriggered());
        }
        long durationNs = System.nanoTime() - startTime;

        double avgNsPerEval = (double) durationNs / iterations;
        double avgMsPerEval = avgNsPerEval / 1_000_000.0;

        System.out.printf("BENCHMARK RESULT: 100,000 evaluations took %.2f ms (Average %.4f ms / %.0f ns per evaluation)%n",
                (double) durationNs / 1_000_000.0, avgMsPerEval, avgNsPerEval);

        // Verification target: rule evaluation should take less than 0.05ms (50 microseconds) per call
        assertTrue(avgMsPerEval < 0.05, "Evaluation latency target exceeded: " + avgMsPerEval + " ms");
    }
}
