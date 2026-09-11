package io.github.incidentdetector.core;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.api.ObservationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Isolated Publisher Manager executing registered ObservationPublisher exports safely.
 */
public class PublisherManager {

    private static final Logger log = LoggerFactory.getLogger(PublisherManager.class);

    private final List<ObservationPublisher> publishers = new CopyOnWriteArrayList<>();

    public PublisherManager(List<ObservationPublisher> publishers) {
        if (publishers != null) {
            this.publishers.addAll(publishers);
        }
    }

    public PublisherManager() {
    }

    public void registerPublisher(ObservationPublisher publisher) {
        if (publisher != null) {
            publishers.add(publisher);
        }
    }

    public void publish(IncidentObservation observation, DetectorHealth health) {
        if (observation == null) return;
        for (ObservationPublisher publisher : publishers) {
            try {
                publisher.publish(observation);
            } catch (Exception e) {
                if (health != null) health.recordPublisherError();
                log.error("Failed to publish observation to [{}]", publisher.getClass().getSimpleName(), e);
            }
        }
    }

    public List<ObservationPublisher> getPublishers() {
        return Collections.unmodifiableList(publishers);
    }
}
