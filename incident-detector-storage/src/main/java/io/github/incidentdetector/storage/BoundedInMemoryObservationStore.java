package io.github.incidentdetector.storage;

import io.github.incidentdetector.api.IncidentObservation;
import io.github.incidentdetector.api.ObservationQuery;
import io.github.incidentdetector.api.ObservationStore;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Thread-safe, bounded, in-memory ObservationStore with TTL eviction.
 */
public class BoundedInMemoryObservationStore implements ObservationStore {

    private final int maxCapacity;
    private final Duration ttl;
    private final Map<String, IncidentObservation> store = new ConcurrentHashMap<>();
    private final Deque<String> insertionOrder = new ConcurrentLinkedDeque<>();

    public BoundedInMemoryObservationStore(int maxCapacity, Duration ttl) {
        this.maxCapacity = maxCapacity > 0 ? maxCapacity : 1000;
        this.ttl = ttl != null ? ttl : Duration.ofHours(24);
    }

    public BoundedInMemoryObservationStore() {
        this(1000, Duration.ofHours(24));
    }

    @Override
    public synchronized void save(IncidentObservation observation) {
        if (observation == null || observation.getId() == null) return;

        evictExpired();

        if (!store.containsKey(observation.getId())) {
            insertionOrder.addLast(observation.getId());
        }
        store.put(observation.getId(), observation);

        while (insertionOrder.size() > maxCapacity) {
            String oldestId = insertionOrder.pollFirst();
            if (oldestId != null) {
                store.remove(oldestId);
            }
        }
    }

    @Override
    public Optional<IncidentObservation> findById(String id) {
        evictExpired();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<IncidentObservation> query(ObservationQuery query) {
        evictExpired();
        if (query == null) return new ArrayList<>(store.values());

        return store.values().stream()
                .filter(obs -> query.getPattern().map(p -> p.equalsIgnoreCase(obs.getPattern())).orElse(true))
                .filter(obs -> query.getMinSeverity().map(s -> obs.getSeverity().ordinal() >= s.ordinal()).orElse(true))
                .filter(obs -> query.getState().map(s -> s == obs.getState()).orElse(true))
                .filter(obs -> query.getStartTime().map(t -> !obs.getDetectedAt().isBefore(t)).orElse(true))
                .filter(obs -> query.getEndTime().map(t -> !obs.getDetectedAt().isAfter(t)).orElse(true))
                .sorted(Comparator.comparing(IncidentObservation::getDetectedAt).reversed())
                .limit(query.getLimit())
                .collect(Collectors.toList());
    }

    @Override
    public int size() {
        evictExpired();
        return store.size();
    }

    @Override
    public synchronized void clear() {
        store.clear();
        insertionOrder.clear();
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minus(ttl);
        Iterator<Map.Entry<String, IncidentObservation>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, IncidentObservation> entry = iterator.next();
            if (entry.getValue().getDetectedAt().isBefore(cutoff)) {
                insertionOrder.remove(entry.getKey());
                iterator.remove();
            }
        }
    }
}
