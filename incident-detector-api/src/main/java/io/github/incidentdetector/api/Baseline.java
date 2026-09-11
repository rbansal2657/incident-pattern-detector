package io.github.incidentdetector.api;

import java.time.Duration;

/**
 * Accessor for statistical baseline data (moving average, percentiles, rate of change, stddev)
 * over configurable sliding windows.
 */
public interface Baseline {

    Double getMovingAverage(String metric, Duration window);

    Double getPercentile(String metric, double percentile, Duration window);

    Double getRateOfChange(String metric, Duration window);

    Double getBaselineValue(String metric);

    Double getStandardDeviation(String metric, Duration window);
}
