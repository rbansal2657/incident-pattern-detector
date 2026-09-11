package io.github.incidentdetector.core;

import io.github.incidentdetector.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active incident lifecycles (DETECTED -> ONGOING -> RESOLVED), deduplication, cooldowns, and hysteresis.
 */
public class IncidentLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleManager.class);

    private final Duration cooldownWindow;
    private final Map<String, ActiveIncident> activeIncidents = new ConcurrentHashMap<>();

    public IncidentLifecycleManager(Duration cooldownWindow) {
        this.cooldownWindow = cooldownWindow != null ? cooldownWindow : Duration.ofSeconds(30);
    }

    public IncidentLifecycleManager() {
        this(Duration.ofSeconds(30));
    }

    public static class ActiveIncident {
        private final String id;
        private final String pattern;
        private final Map<String, String> dimensions;
        private final Instant detectedAt;
        private Instant lastUpdatedAt;
        private Instant lastPublishedAt;
        private Severity severity;
        private double confidence;
        private DetectionResult lastResult;

        public ActiveIncident(String id, String pattern, Map<String, String> dimensions, Instant detectedAt, Severity severity, double confidence, DetectionResult result) {
            this.id = id;
            this.pattern = pattern;
            this.dimensions = dimensions != null ? Map.copyOf(dimensions) : Collections.emptyMap();
            this.detectedAt = detectedAt;
            this.lastUpdatedAt = detectedAt;
            this.lastPublishedAt = detectedAt;
            this.severity = severity;
            this.confidence = confidence;
            this.lastResult = result;
        }

        public String getId() { return id; }
        public String getPattern() { return pattern; }
        public Map<String, String> getDimensions() { return dimensions; }
        public Instant getDetectedAt() { return detectedAt; }
        public Instant getLastUpdatedAt() { return lastUpdatedAt; }
        public Instant getLastPublishedAt() { return lastPublishedAt; }
        public Severity getSeverity() { return severity; }
        public double getConfidence() { return confidence; }
        public DetectionResult getLastResult() { return lastResult; }
    }

    public Optional<IncidentObservation> processEvaluation(String pattern, DetectionResult result, Instant now) {
        String key = buildIncidentKey(pattern, result.getDimensions());
        ActiveIncident active = activeIncidents.get(key);
        if (active == null && (!result.isTriggered() || result.getDimensions().isEmpty())) {
            for (Map.Entry<String, ActiveIncident> entry : activeIncidents.entrySet()) {
                if (entry.getValue().getPattern().equalsIgnoreCase(pattern)) {
                    key = entry.getKey();
                    active = entry.getValue();
                    break;
                }
            }
        }

        if (result.isTriggered()) {
            if (active == null) {
                // New incident detected
                String id = UUID.randomUUID().toString();
                ActiveIncident newActive = new ActiveIncident(id, pattern, result.getDimensions(), now, result.getSeverity(), result.getConfidence(), result);
                activeIncidents.put(key, newActive);

                log.info("INCIDENT_DETECTED pattern={} severity={} confidence={:.2f} dimensions={}",
                        pattern, result.getSeverity(), result.getConfidence(), result.getDimensions());

                IncidentObservation observation = IncidentObservation.builder()
                        .id(id)
                        .pattern(pattern)
                        .severity(result.getSeverity())
                        .state(IncidentState.DETECTED)
                        .confidence(result.getConfidence())
                        .detectedAt(now)
                        .lastUpdatedAt(now)
                        .observationWindow(Duration.ofMinutes(1))
                        .summary(result.getSummary())
                        .evidence(result.getEvidence())
                        .correlations(result.getCorrelations())
                        .probableCauses(result.getProbableCauses())
                        .potentialImpacts(result.getPotentialImpacts())
                        .recommendedActions(result.getRecommendedActions())
                        .dimensions(result.getDimensions())
                        .build();

                return Optional.of(observation);
            } else {
                // Ongoing incident
                active.lastUpdatedAt = now;
                boolean severityEscalated = result.getSeverity().ordinal() > active.severity.ordinal();
                boolean cooldownExpired = Duration.between(active.lastPublishedAt, now).compareTo(cooldownWindow) >= 0;

                active.severity = result.getSeverity();
                active.confidence = result.getConfidence();
                active.lastResult = result;

                if (severityEscalated || cooldownExpired) {
                    active.lastPublishedAt = now;
                    log.info("INCIDENT_ONGOING pattern={} severity={} confidence={:.2f}", pattern, result.getSeverity(), result.getConfidence());

                    IncidentObservation observation = IncidentObservation.builder()
                            .id(active.getId())
                            .pattern(pattern)
                            .severity(result.getSeverity())
                            .state(IncidentState.ONGOING)
                            .confidence(result.getConfidence())
                            .detectedAt(active.getDetectedAt())
                            .lastUpdatedAt(now)
                            .observationWindow(Duration.between(active.getDetectedAt(), now))
                            .summary(result.getSummary() + " [ONGOING]")
                            .evidence(result.getEvidence())
                            .correlations(result.getCorrelations())
                            .probableCauses(result.getProbableCauses())
                            .potentialImpacts(result.getPotentialImpacts())
                            .recommendedActions(result.getRecommendedActions())
                            .dimensions(result.getDimensions())
                            .build();

                    return Optional.of(observation);
                } else {
                    // Suppress duplicate observation within cooldown window
                    return Optional.empty();
                }
            }
        } else {
            // Not triggered
            if (active != null) {
                // Resolved!
                activeIncidents.remove(key);

                log.info("INCIDENT_RESOLVED pattern={} duration={}s", pattern, Duration.between(active.getDetectedAt(), now).toSeconds());

                List<Evidence> resolutionEvidence = buildResolutionEvidence(active.getLastResult(), result);

                IncidentObservation observation = IncidentObservation.builder()
                        .id(active.getId())
                        .pattern(pattern)
                        .severity(active.getSeverity())
                        .state(IncidentState.RESOLVED)
                        .confidence(1.0)
                        .detectedAt(active.getDetectedAt())
                        .lastUpdatedAt(now)
                        .resolvedAt(now)
                        .observationWindow(Duration.between(active.getDetectedAt(), now))
                        .summary(pattern + " incident has RESOLVED after " + Duration.between(active.getDetectedAt(), now).toSeconds() + " seconds.")
                        .evidence(resolutionEvidence)
                        .correlations(Collections.emptyList())
                        .probableCauses(Collections.emptyList())
                        .potentialImpacts(Collections.emptyList())
                        .recommendedActions(List.of("Verify metrics remain within nominal operational thresholds."))
                        .dimensions(active.getDimensions())
                        .build();

                return Optional.of(observation);
            }
            return Optional.empty();
        }
    }

    private List<Evidence> buildResolutionEvidence(DetectionResult beforeResult, DetectionResult currentResult) {
        List<Evidence> list = new ArrayList<>();
        if (beforeResult != null && beforeResult.getEvidence() != null) {
            for (Evidence e : beforeResult.getEvidence()) {
                list.add(new Evidence(
                        e.getMetric(),
                        "RESOLVED (was " + e.getCurrentValue() + ")",
                        e.getBaselineValue(),
                        e.getThreshold(),
                        e.getUnit(),
                        "RECOVERED",
                        e.getDuration(),
                        1.0
                ));
            }
        }
        return list;
    }

    private String buildIncidentKey(String pattern, Map<String, String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return pattern;
        }
        TreeMap<String, String> sorted = new TreeMap<>(dimensions);
        return pattern + ":" + sorted.toString();
    }

    public Map<String, ActiveIncident> getActiveIncidents() {
        return Collections.unmodifiableMap(activeIncidents);
    }

    public void clear() {
        activeIncidents.clear();
    }
}
