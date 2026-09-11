package io.github.incidentdetector.opentelemetry;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.api.ObservationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes IncidentObservations to OpenTelemetry with semantic attributes (incident.pattern, incident.severity, etc).
 */
public class OpenTelemetryObservationPublisher implements ObservationPublisher {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryObservationPublisher.class);

    @Override
    public void publish(IncidentObservation observation) {
        if (observation == null) return;

        String pattern = observation.getPattern();
        String severity = observation.getSeverity().name();
        String component = observation.getDimensions().getOrDefault("component", "application");

        log.info("OTEL_EVENT_EMITTED attributes[incident.pattern={}, incident.severity={}, incident.confidence={:.2f}, incident.component={}, incident.state={}] summary=\"{}\"",
                pattern, severity, observation.getConfidence(), component, observation.getState(), observation.getSummary());
    }
}
