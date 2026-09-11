package io.github.incidentdetector.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Health and operational statistics collector for the Incident Detector pipeline.
 */
public class DetectorHealth {

    private final AtomicLong evaluationsTotal = new AtomicLong(0);
    private final AtomicLong evaluationDurationTotalMs = new AtomicLong(0);
    private final AtomicLong observationsTotal = new AtomicLong(0);
    private final AtomicLong ruleErrorsTotal = new AtomicLong(0);
    private final AtomicLong publisherErrorsTotal = new AtomicLong(0);
    private final AtomicLong droppedEventsTotal = new AtomicLong(0);
    private final AtomicLong queueSize = new AtomicLong(0);

    public void recordEvaluation(long durationMs) {
        evaluationsTotal.incrementAndGet();
        evaluationDurationTotalMs.addAndGet(durationMs);
    }

    public void recordObservation() {
        observationsTotal.incrementAndGet();
    }

    public void recordRuleError() {
        ruleErrorsTotal.incrementAndGet();
    }

    public void recordPublisherError() {
        publisherErrorsTotal.incrementAndGet();
    }

    public void recordDroppedEvent() {
        droppedEventsTotal.incrementAndGet();
    }

    public void setQueueSize(long size) {
        queueSize.set(size);
    }

    public long getEvaluationsTotal() {
        return evaluationsTotal.get();
    }

    public long getEvaluationDurationTotalMs() {
        return evaluationDurationTotalMs.get();
    }

    public double getAverageEvaluationDurationMs() {
        long total = evaluationsTotal.get();
        return total > 0 ? (double) evaluationDurationTotalMs.get() / total : 0.0;
    }

    public long getObservationsTotal() {
        return observationsTotal.get();
    }

    public long getRuleErrorsTotal() {
        return ruleErrorsTotal.get();
    }

    public long getPublisherErrorsTotal() {
        return publisherErrorsTotal.get();
    }

    public long getDroppedEventsTotal() {
        return droppedEventsTotal.get();
    }

    public long getQueueSize() {
        return queueSize.get();
    }

    @Override
    public String toString() {
        return String.format("DetectorHealth[evaluations=%d, observations=%d, ruleErrors=%d, publisherErrors=%d, droppedEvents=%d, avgDuration=%.2fms]",
                evaluationsTotal.get(), observationsTotal.get(), ruleErrorsTotal.get(), publisherErrorsTotal.get(), droppedEventsTotal.get(), getAverageEvaluationDurationMs());
    }
}
