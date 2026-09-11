package io.github.incidentdetector.examples;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.core.IncidentDetector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trigger")
public class TriggerController {

    private final IncidentDetector detector;

    public TriggerController(IncidentDetector detector) {
        this.detector = detector;
    }

    @PostMapping("/pool-saturation")
    public ResponseEntity<List<IncidentObservation>> triggerPoolSaturation() {
        detector.recordTelemetry("pool.utilization", 0.98);
        detector.recordTelemetry("pool.pending_acquisitions", 43);
        detector.recordTelemetry("pool.acquire_latency_p95", 740);
        detector.recordTelemetry("request.latency_p95", 920);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/retry-storm")
    public ResponseEntity<List<IncidentObservation>> triggerRetryStorm() {
        detector.recordTelemetry("http.retry_ratio", 0.45);
        detector.recordTelemetry("http.retry_rate", 240);
        detector.recordTelemetry("dependency.error_rate", 0.35);
        detector.recordTelemetry("request.latency_p95", 850);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/timeout-cascade")
    public ResponseEntity<List<IncidentObservation>> triggerTimeoutCascade() {
        detector.recordTelemetry("dependency.latency_p95", 950);
        detector.recordTelemetry("dependency.timeout_count", 28);
        detector.recordTelemetry("upstream.timeout_count", 19);
        detector.recordTelemetry("executor.active_threads", 50);
        detector.recordTelemetry("request.latency_p95", 1400);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/hot-key")
    public ResponseEntity<List<IncidentObservation>> triggerHotKey() {
        detector.recordKeyDistribution("redis", Map.of(
                "user_session_token_super_secret_998877", 850.0,
                "key_normal_1", 20.0,
                "key_normal_2", 15.0
        ));

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/cache-thrashing")
    public ResponseEntity<List<IncidentObservation>> triggerCacheThrashing() {
        detector.recordTelemetry("cache.hit_ratio", 0.18);
        detector.recordTelemetry("cache.eviction_rate", 1200);
        detector.recordTelemetry("backend.request_rate", 850);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/thread-pool-starvation")
    public ResponseEntity<List<IncidentObservation>> triggerThreadPoolStarvation() {
        detector.recordTelemetry("executor.active_threads", 50);
        detector.recordTelemetry("executor.max_threads", 50);
        detector.recordTelemetry("executor.queue_depth", 340);
        detector.recordTelemetry("executor.task_wait_time_p95", 890);
        detector.recordTelemetry("executor.rejected_tasks", 12);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/consumer-lag")
    public ResponseEntity<List<IncidentObservation>> triggerConsumerLag() {
        detector.recordTelemetry("kafka.consumer_lag", 18500);
        detector.recordTelemetry("kafka.lag_growth_rate", 140);
        detector.recordTelemetry("kafka.processing_latency_p95", 650);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/large-payload")
    public ResponseEntity<List<IncidentObservation>> triggerLargePayload() {
        detector.recordTelemetry("payload.size_p95", 12 * 1024 * 1024); // 12MB
        detector.recordTelemetry("payload.size_max", 25 * 1024 * 1024);
        detector.recordTelemetry("payload.serialization_latency_p95", 450);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/gc-latency")
    public ResponseEntity<List<IncidentObservation>> triggerGcLatency() {
        detector.recordTelemetry("jvm.gc.pause_duration", 1250);
        detector.recordTelemetry("jvm.gc.full_gc_count", 3);
        detector.recordTelemetry("jvm.memory.heap_utilization", 0.96);
        detector.recordTelemetry("request.latency_p95", 1600);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/dependency-degradation")
    public ResponseEntity<List<IncidentObservation>> triggerDependencyDegradation() {
        detector.recordTelemetry("dependency.latency_p95", 850);
        detector.recordTelemetry("dependency.error_rate", 0.28);
        detector.recordTelemetry("dependency.timeout_rate", 0.12);
        detector.recordTelemetry("request.latency_p95", 1100);

        List<IncidentObservation> observations = detector.evaluateNow();
        return ResponseEntity.ok(observations);
    }

    @PostMapping("/clear")
    public ResponseEntity<List<IncidentObservation>> clearAllIncidents() {
        detector.recordTelemetry("pool.utilization", 0.20);
        detector.recordTelemetry("pool.pending_acquisitions", 0);
        detector.recordTelemetry("pool.acquire_latency_p95", 5);
        detector.recordTelemetry("request.latency_p95", 15);

        detector.recordTelemetry("http.retry_ratio", 0.01);
        detector.recordTelemetry("http.retry_rate", 1);
        detector.recordTelemetry("dependency.error_rate", 0.001);

        detector.recordTelemetry("dependency.timeout_count", 0);
        detector.recordTelemetry("upstream.timeout_count", 0);

        detector.recordKeyDistribution("redis", Map.of("key1", 10.0, "key2", 10.0));

        detector.recordTelemetry("cache.hit_ratio", 0.95);
        detector.recordTelemetry("cache.eviction_rate", 0);
        detector.recordTelemetry("backend.request_rate", 10);

        detector.recordTelemetry("executor.active_threads", 5);
        detector.recordTelemetry("executor.max_threads", 50);
        detector.recordTelemetry("executor.queue_depth", 0);
        detector.recordTelemetry("executor.task_wait_time_p95", 1);
        detector.recordTelemetry("executor.rejected_tasks", 0);

        detector.recordTelemetry("kafka.consumer_lag", 0);
        detector.recordTelemetry("kafka.lag_growth_rate", 0);
        detector.recordTelemetry("kafka.processing_latency_p95", 5);

        detector.recordTelemetry("payload.size_p95", 10 * 1024);
        detector.recordTelemetry("payload.serialization_latency_p95", 2);

        detector.recordTelemetry("jvm.gc.pause_duration", 5);
        detector.recordTelemetry("jvm.gc.full_gc_count", 0);
        detector.recordTelemetry("jvm.memory.heap_utilization", 0.40);

        detector.recordTelemetry("dependency.latency_p95", 10);
        detector.recordTelemetry("dependency.timeout_rate", 0.0);

        List<IncidentObservation> resolutionObservations = detector.evaluateNow();
        return ResponseEntity.ok(resolutionObservations);
    }
}
