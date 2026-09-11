package io.github.incidentdetector.api;

/**
 * Publisher abstraction for exporting generated IncidentObservations to downstream systems.
 */
public interface ObservationPublisher {

    void publish(IncidentObservation observation);
}
