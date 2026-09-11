package io.github.incidentdetector.core;

import io.github.incidentdetector.api.Correlation;
import io.github.incidentdetector.api.TelemetrySnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight temporal correlation engine for discovering lead-lag relationships between signals.
 */
public class CorrelationEngine {

    public List<Correlation> analyzeCorrelations(List<TelemetrySnapshot> snapshots, String sourceMetric, String targetMetric, Duration maxWindow) {
        List<Correlation> results = new ArrayList<>();
        if (snapshots == null || snapshots.size() < 3) return results;

        int leadStep = -1;
        double maxCorrelation = 0.0;

        for (int step = 0; step < Math.min(5, snapshots.size() - 2); step++) {
            double corr = calculateLaggedCorrelation(snapshots, sourceMetric, targetMetric, step);
            if (corr > maxCorrelation && corr > 0.4) {
                maxCorrelation = corr;
                leadStep = step;
            }
        }

        if (leadStep >= 0 && maxCorrelation > 0.4) {
            long lagMillis = leadStep * 1000L;
            String explanation = String.format("%s increased approximately %dms before %s increased",
                    sourceMetric, lagMillis, targetMetric);
            results.add(new Correlation(sourceMetric, targetMetric, Duration.ofMillis(lagMillis), maxCorrelation, explanation));
        }

        return results;
    }

    private double calculateLaggedCorrelation(List<TelemetrySnapshot> snapshots, String sourceMetric, String targetMetric, int lag) {
        List<Double> sourceVals = new ArrayList<>();
        List<Double> targetVals = new ArrayList<>();

        for (int i = 0; i < snapshots.size() - lag; i++) {
            Double s = snapshots.get(i).getMetric(sourceMetric);
            Double t = snapshots.get(i + lag).getMetric(targetMetric);
            if (s != null && t != null) {
                sourceVals.add(s);
                targetVals.add(t);
            }
        }

        if (sourceVals.size() < 3) return 0.0;

        double meanS = sourceVals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanT = targetVals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double num = 0.0;
        double denS = 0.0;
        double denT = 0.0;

        for (int i = 0; i < sourceVals.size(); i++) {
            double diffS = sourceVals.get(i) - meanS;
            double diffT = targetVals.get(i) - meanT;
            num += diffS * diffT;
            denS += diffS * diffS;
            denT += diffT * diffT;
        }

        if (denS == 0.0 || denT == 0.0) return 0.0;
        return num / Math.sqrt(denS * denT);
    }
}
