package io.github.incidentdetector.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured, deterministic, explainable observation produced by the Incident Pattern Detector.
 */
public class IncidentObservation {

    private final String id;
    private final String pattern;
    private final Severity severity;
    private final IncidentState state;
    private final double confidence;

    private final Instant detectedAt;
    private final Instant lastUpdatedAt;
    private final Instant resolvedAt;
    private final Duration observationWindow;

    private final String summary;
    private final List<Evidence> evidence;
    private final List<Correlation> correlations;
    private final List<String> probableCauses;
    private final List<String> potentialImpacts;
    private final List<String> recommendedActions;

    private final Map<String, String> dimensions;

    public IncidentObservation(Builder builder) {
        this.id = builder.id;
        this.pattern = builder.pattern;
        this.severity = builder.severity;
        this.state = builder.state != null ? builder.state : IncidentState.DETECTED;
        this.confidence = builder.confidence;
        this.detectedAt = builder.detectedAt != null ? builder.detectedAt : Instant.now();
        this.lastUpdatedAt = builder.lastUpdatedAt != null ? builder.lastUpdatedAt : this.detectedAt;
        this.resolvedAt = builder.resolvedAt;
        this.observationWindow = builder.observationWindow != null ? builder.observationWindow : Duration.ofMinutes(1);
        this.summary = builder.summary;
        this.evidence = builder.evidence != null ? List.copyOf(builder.evidence) : Collections.emptyList();
        this.correlations = builder.correlations != null ? List.copyOf(builder.correlations) : Collections.emptyList();
        this.probableCauses = builder.probableCauses != null ? List.copyOf(builder.probableCauses) : Collections.emptyList();
        this.potentialImpacts = builder.potentialImpacts != null ? List.copyOf(builder.potentialImpacts) : Collections.emptyList();
        this.recommendedActions = builder.recommendedActions != null ? List.copyOf(builder.recommendedActions) : Collections.emptyList();
        this.dimensions = builder.dimensions != null ? Map.copyOf(builder.dimensions) : Collections.emptyMap();
    }

    public String getId() {
        return id;
    }

    public String getPattern() {
        return pattern;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IncidentState getState() {
        return state;
    }

    public double getConfidence() {
        return confidence;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Duration getObservationWindow() {
        return observationWindow;
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
        private String id;
        private String pattern;
        private Severity severity = Severity.MEDIUM;
        private IncidentState state = IncidentState.DETECTED;
        private double confidence;
        private Instant detectedAt;
        private Instant lastUpdatedAt;
        private Instant resolvedAt;
        private Duration observationWindow;
        private String summary;
        private List<Evidence> evidence;
        private List<Correlation> correlations;
        private List<String> probableCauses;
        private List<String> potentialImpacts;
        private List<String> recommendedActions;
        private Map<String, String> dimensions;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder state(IncidentState state) {
            this.state = state;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }

        public Builder lastUpdatedAt(Instant lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder observationWindow(Duration observationWindow) {
            this.observationWindow = observationWindow;
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

        public IncidentObservation build() {
            return new IncidentObservation(this);
        }
    }
}
