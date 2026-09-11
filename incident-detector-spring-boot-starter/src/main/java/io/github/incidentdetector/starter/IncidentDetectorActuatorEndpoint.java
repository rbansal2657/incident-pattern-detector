package io.github.incidentdetector.starter;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.api.ObservationQuery;
import io.github.incidentdetector.api.ObservationStore;
import io.github.incidentdetector.core.IncidentDetector;
import io.github.incidentdetector.core.IncidentLifecycleManager;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Endpoint(id = "incident-detector")
public class IncidentDetectorActuatorEndpoint {

    private final IncidentDetector detector;
    private final ObservationStore observationStore;

    public IncidentDetectorActuatorEndpoint(IncidentDetector detector, ObservationStore observationStore) {
        this.detector = detector;
        this.observationStore = observationStore;
    }

    @ReadOperation
    public Map<String, Object> getDiagnostics() {
        Map<String, Object> response = new HashMap<>();

        response.put("health", detector.getHealth().toString());

        Map<String, Object> activeIncidentsMap = new HashMap<>();
        for (Map.Entry<String, IncidentLifecycleManager.ActiveIncident> entry : detector.getActiveIncidents().entrySet()) {
            Map<String, Object> details = new HashMap<>();
            details.put("id", entry.getValue().getId());
            details.put("pattern", entry.getValue().getPattern());
            details.put("severity", entry.getValue().getSeverity());
            details.put("confidence", entry.getValue().getConfidence());
            details.put("detectedAt", entry.getValue().getDetectedAt());
            details.put("dimensions", entry.getValue().getDimensions());
            activeIncidentsMap.put(entry.getKey(), details);
        }
        response.put("activeIncidents", activeIncidentsMap);

        if (observationStore != null) {
            List<IncidentObservation> recent = observationStore.query(ObservationQuery.builder().limit(10).build());
            response.put("recentObservations", recent);
        }

        return response;
    }
}
