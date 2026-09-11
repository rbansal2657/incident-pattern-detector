package io.github.incidentdetector.core;

import io.github.incidentdetector.api.Baseline;
import io.github.incidentdetector.api.TelemetrySnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding window baseline calculator that maintains a bounded queue of historical TelemetrySnapshots.
 */
public class SlidingWindowBaselineEngine implements Baseline {

    private final Duration maxWindow;
    private final int maxSnapshots;
    private final Deque<TelemetrySnapshot> history = new ConcurrentLinkedDeque<>();

    public SlidingWindowBaselineEngine(Duration maxWindow, int maxSnapshots) {
        this.maxWindow = maxWindow != null ? maxWindow : Duration.ofMinutes(15);
        this.maxSnapshots = maxSnapshots > 0 ? maxSnapshots : 1000;
    }

    public SlidingWindowBaselineEngine() {
        this(Duration.ofMinutes(15), 1000);
    }

    public void recordSnapshot(TelemetrySnapshot snapshot) {
        if (snapshot == null) return;
        history.addLast(snapshot);
        evictOldSnapshots(snapshot.getTimestamp());
    }

    private void evictOldSnapshots(Instant now) {
        Instant cutoff = now.minus(maxWindow);
        while (!history.isEmpty()) {
            TelemetrySnapshot first = history.peekFirst();
            if (first != null && (first.getTimestamp().isBefore(cutoff) || history.size() > maxSnapshots)) {
                history.pollFirst();
            } else {
                break;
            }
        }
    }

    @Override
    public Double getMovingAverage(String metric, Duration window) {
        List<Double> values = getValuesInWindow(metric, window);
        if (values.isEmpty()) return null;
        double sum = 0;
        for (Double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    @Override
    public Double getPercentile(String metric, double percentile, Duration window) {
        List<Double> values = getValuesInWindow(metric, window);
        if (values.isEmpty()) return null;
        Collections.sort(values);
        if (percentile <= 0) return values.get(0);
        if (percentile >= 100) return values.get(values.size() - 1);
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }

    @Override
    public Double getRateOfChange(String metric, Duration window) {
        List<TelemetrySnapshot> snapshots = getSnapshotsInWindow(window);
        if (snapshots.size() < 2) return 0.0;
        
        Double firstVal = snapshots.get(0).getMetric(metric);
        Double lastVal = snapshots.get(snapshots.size() - 1).getMetric(metric);

        if (firstVal == null || lastVal == null) return 0.0;
        if (firstVal == 0.0) return lastVal > 0 ? 1.0 : 0.0;

        return (lastVal - firstVal) / Math.abs(firstVal);
    }

    @Override
    public Double getBaselineValue(String metric) {
        return getMovingAverage(metric, Duration.ofMinutes(5));
    }

    @Override
    public Double getStandardDeviation(String metric, Duration window) {
        List<Double> values = getValuesInWindow(metric, window);
        if (values.size() < 2) return 0.0;
        double avg = getMovingAverage(metric, window);
        double varianceSum = 0;
        for (Double v : values) {
            varianceSum += Math.pow(v - avg, 2);
        }
        return Math.sqrt(varianceSum / values.size());
    }

    public List<TelemetrySnapshot> getSnapshotsInWindow(Duration window) {
        Instant cutoff = Instant.now().minus(window != null ? window : maxWindow);
        List<TelemetrySnapshot> result = new ArrayList<>();
        for (TelemetrySnapshot s : history) {
            if (!s.getTimestamp().isBefore(cutoff)) {
                result.add(s);
            }
        }
        return result;
    }

    private List<Double> getValuesInWindow(String metric, Duration window) {
        List<TelemetrySnapshot> snapshots = getSnapshotsInWindow(window);
        List<Double> values = new ArrayList<>();
        for (TelemetrySnapshot s : snapshots) {
            Double val = s.getMetric(metric);
            if (val != null) {
                values.add(val);
            }
        }
        return values;
    }

    public int snapshotCount() {
        return history.size();
    }

    public void clear() {
        history.clear();
    }
}
