package io.github.incidentdetector.api;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for retaining and querying recent IncidentObservations locally.
 */
public interface ObservationStore {

    void save(IncidentObservation observation);

    Optional<IncidentObservation> findById(String id);

    List<IncidentObservation> query(ObservationQuery query);

    int size();

    void clear();
}
