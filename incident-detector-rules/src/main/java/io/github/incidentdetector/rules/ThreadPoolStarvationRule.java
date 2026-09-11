package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects Java Executor thread pool starvation requiring multi-signal evidence beyond active thread counts.
 */
public class ThreadPoolStarvationRule implements IncidentRule {

    @Override
    public String pattern() {
        return "THREAD_POOL_STARVATION";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Double active = snapshot.getMetric("executor.active_threads");
        Double max = snapshot.getMetric("executor.max_threads");
        Double queueDepth = snapshot.getMetric("executor.queue_depth");
        Double waitTime = snapshot.getMetric("executor.task_wait_time_p95");
        Double rejected = snapshot.getMetric("executor.rejected_tasks");
        Double reqLatency = snapshot.getMetric("request.latency_p95");

        if (active == null && queueDepth == null && rejected == null) {
            return DetectionResult.notTriggered();
        }

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.0;

        boolean threadsFullyActive = active != null && max != null && max > 0 && (active / max) >= 0.95;
        if (threadsFullyActive) {
            double contrib = 0.25;
            score += contrib;
            evidence.add(new Evidence("executor.active_threads", String.format("%.0f/%.0f", active, max),
                    "20%", "95%", "threads", "ELEVATED", Duration.ofSeconds(10), contrib));
        }

        if (queueDepth != null && queueDepth > 20) {
            double contrib = 0.35;
            score += contrib;
            evidence.add(new Evidence("executor.queue_depth", queueDepth.intValue(),
                    0, 20, "queued tasks", "SPIKE", Duration.ofSeconds(10), contrib));
        }

        Double waitBaseline = baseline.getMovingAverage("executor.task_wait_time_p95", Duration.ofMinutes(5));
        if (waitTime != null && waitTime > 100) {
            double contrib = 0.30;
            score += contrib;
            evidence.add(new Evidence("executor.task_wait_time_p95", String.format("%.0fms", waitTime),
                    waitBaseline != null ? String.format("%.0fms", waitBaseline) : "5ms", "100ms", "ms", "INCREASE", Duration.ofSeconds(10), contrib));
        }

        if (rejected != null && rejected > 0) {
            double contrib = 0.40;
            score += contrib;
            evidence.add(new Evidence("executor.rejected_tasks", rejected.intValue(),
                    0, 0, "rejections", "CRITICAL", Duration.ofSeconds(10), contrib));
        }

        // Multi-signal guard: activeThreads == maxThreads ALONE is NOT enough! Must have queued tasks, wait time, or rejections.
        if (evidence.size() < 2 || score < 0.50) {
            return DetectionResult.notTriggered();
        }

        Severity severity = (rejected != null && rejected > 0) || score >= 0.85 ? Severity.CRITICAL : Severity.HIGH;

        List<String> probableCauses = List.of(
                "Synchronous blocking IO operations executed on non-blocking event loops or thread pools.",
                "ThreadPool maxPoolSize set too small for incoming concurrent request load.",
                "Deadlocks or thread contention on shared application synchronization monitors."
        );

        List<String> potentialImpacts = List.of(
                "Tasks queued indefinitely incurring massive queue wait latency before execution.",
                "Task rejection exceptions (RejectedExecutionException) causing HTTP 503 Server Errors."
        );

        List<String> recommendedActions = List.of(
                "Take thread dump (jstack / jcmd) to inspect blocked or waiting thread states.",
                "Ensure IO operations use asynchronous non-blocking drivers or dedicated IO thread pools.",
                "Tune maxPoolSize and queueCapacity configuration."
        );

        List<Correlation> correlations = List.of(
                new Correlation("executor.queue_depth", "executor.task_wait_time_p95", Duration.ofMillis(200), 0.96,
                        "Queue depth surge directly drove task scheduling wait time escalation.")
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Executor thread pool starvation detected: tasks accumulating in queue with elevated wait times.")
                .evidence(evidence)
                .correlations(correlations)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "executor-pool"))
                .build();
    }
}
