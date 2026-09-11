package io.github.incidentdetector.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result returned by an IncidentRule evaluation step.
 */
public class DetectionResult {

    private final boolean triggered;
    private final double confidence;
    private final Severity severity;
    private final String summary;
    private final List<Evidence> evidence;
    private final List<Correlation> correlations;
    private final List<String> probableCauses;
    private final List<String> potentialImpacts;
    private final List<String> recommendedActions;
    private final Map<String, String> dimensions;

    private DetectionResult(Builder builder) {
        this.triggered = builder.triggered;
        this.confidence = builder.confidence;
        this.severity = builder.severity;
        this.summary = builder.summary;
        this.evidence = builder.evidence != null ? List.copyOf(builder.evidence) : Collections.emptyList();
        this.correlations = builder.correlations != null ? List.copyOf(builder.correlations) : Collections.emptyList();
        this.probableCauses = builder.probableCauses != null ? List.copyOf(builder.probableCauses) : Collections.emptyList();
        this.potentialImpacts = builder.potentialImpacts != null ? List.copyOf(builder.potentialImpacts) : Collections.emptyList();
        this.recommendedActions = builder.recommendedActions != null ? List.copyOf(builder.recommendedActions) : Collections.emptyList();
        this.dimensions = builder.dimensions != null ? Map.copyOf(builder.dimensions) : Collections.emptyMap();
    }

    public static DetectionResult notTriggered() {
        return builder().triggered(false).confidence(0.0).build();
    }

    public boolean isTriggered() {
        return triggered;
    }

    public double getConfidence() {
        return confidence;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSummary() {
        return summary;
    }

    public List<Evidence> getEvidence() {
        return evidence;
    }

    public List<Correlation> getCorrelations() {
        return correlations;
    }

    public List<String> getProbableCauses() {
        return probableCauses;
    }

    public List<String> getPotentialImpacts() {
        return potentialImpacts;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public Map<String, String> getDimensions() {
        return dimensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean triggered;
        private double confidence;
        private Severity severity = Severity.MEDIUM;
        private String summary = "";
        private List<Evidence> evidence;
        private List<Correlation> correlations;
        private List<String> probableCauses;
        private List<String> potentialImpacts;
        private List<String> recommendedActions;
        private Map<String, String> dimensions;

        public Builder triggered(boolean triggered) {
            this.triggered = triggered;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder evidence(List<Evidence> evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder correlations(List<Correlation> correlations) {
            this.correlations = correlations;
            return this;
        }

        public Builder probableCauses(List<String> probableCauses) {
            this.probableCauses = probableCauses;
            return this;
        }

        public Builder potentialImpacts(List<String> potentialImpacts) {
            this.potentialImpacts = potentialImpacts;
            return this;
        }

        public Builder recommendedActions(List<String> recommendedActions) {
            this.recommendedActions = recommendedActions;
            return this;
        }

        public Builder dimensions(Map<String, String> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public DetectionResult build() {
            return new DetectionResult(this);
        }
    }
}
