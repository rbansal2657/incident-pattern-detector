package io.github.incidentdetector.api;

import java.time.Instant;
import java.util.Optional;

/**
 * Filter query object for searching historical IncidentObservations.
 */
public class ObservationQuery {

    private final String pattern;
    private final Severity minSeverity;
    private final IncidentState state;
    private final Instant startTime;
    private final Instant endTime;
    private final int limit;

    public ObservationQuery(String pattern, Severity minSeverity, IncidentState state, Instant startTime, Instant endTime, int limit) {
        this.pattern = pattern;
        this.minSeverity = minSeverity;
        this.state = state;
        this.startTime = startTime;
        this.endTime = endTime;
        this.limit = limit > 0 ? limit : 100;
    }

    public Optional<String> getPattern() {
        return Optional.ofNullable(pattern);
    }

    public Optional<Severity> getMinSeverity() {
        return Optional.ofNullable(minSeverity);
    }

    public Optional<IncidentState> getState() {
        return Optional.ofNullable(state);
    }

    public Optional<Instant> getStartTime() {
        return Optional.ofNullable(startTime);
    }

    public Optional<Instant> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    public int getLimit() {
        return limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String pattern;
        private Severity minSeverity;
        private IncidentState state;
        private Instant startTime;
        private Instant endTime;
        private int limit = 100;

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder minSeverity(Severity minSeverity) {
            this.minSeverity = minSeverity;
            return this;
        }

        public Builder state(IncidentState state) {
            this.state = state;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public ObservationQuery build() {
            return new ObservationQuery(pattern, minSeverity, state, startTime, endTime, limit);
        }
    }
}
