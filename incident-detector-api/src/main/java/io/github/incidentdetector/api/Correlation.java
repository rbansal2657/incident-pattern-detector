package io.github.incidentdetector.api;

import java.time.Duration;

/**
 * Represents temporal or directional correlation between two telemetry signals.
 */
public class Correlation {

    private final String sourceSignal;
    private final String targetSignal;
    private final Duration timeDifference;
    private final double strength;
    private final String explanation;

    public Correlation(String sourceSignal, String targetSignal, Duration timeDifference, double strength, String explanation) {
        this.sourceSignal = sourceSignal;
        this.targetSignal = targetSignal;
        this.timeDifference = timeDifference;
        this.strength = strength;
        this.explanation = explanation;
    }

    public String getSourceSignal() {
        return sourceSignal;
    }

    public String getTargetSignal() {
        return targetSignal;
    }

    public Duration getTimeDifference() {
        return timeDifference;
    }

    public double getStrength() {
        return strength;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%s, strength: %.2f): %s",
                sourceSignal, targetSignal,
                timeDifference != null ? timeDifference.toMillis() + "ms" : "0ms",
                strength, explanation);
    }
}
