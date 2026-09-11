package io.github.incidentdetector.core;

import io.github.incidentdetector.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main facade and background engine for the Incident Pattern Detector library.
 */
public class IncidentDetector implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(IncidentDetector.class);

    private final TelemetryRegistry telemetryRegistry;
    private final SlidingWindowBaselineEngine baselineEngine;
    private final RuleEngine ruleEngine;
    private final IncidentLifecycleManager lifecycleManager;
    private final PublisherManager publisherManager;
    private final ObservationStore observationStore;
    private final DetectorHealth health;
    private final Duration evaluationInterval;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean running = false;

    private IncidentDetector(Builder builder) {
        this.telemetryRegistry = builder.telemetryRegistry;
        this.baselineEngine = builder.baselineEngine;
        this.ruleEngine = builder.ruleEngine;
        this.lifecycleManager = builder.lifecycleManager;
        this.publisherManager = builder.publisherManager;
        this.observationStore = builder.observationStore;
        this.health = new DetectorHealth();
        this.evaluationInterval = builder.evaluationInterval != null ? builder.evaluationInterval : Duration.ofSeconds(10);

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "incident-detector-analysis");
            t.setDaemon(true);
            return t;
        });

        if (builder.storeInPublisherManager && this.observationStore != null) {
            this.publisherManager.registerPublisher(this.observationStore::save);
        }
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        scheduledFuture = executor.scheduleAtFixedRate(this::runAnalysisCycle,
                0, evaluationInterval.toMillis(), TimeUnit.MILLISECONDS);
        log.info("IncidentDetector started with evaluation interval of {}s", evaluationInterval.toSeconds());
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("IncidentDetector stopped.");
    }

    @Override
    public void close() {
        stop();
    }

    public List<IncidentObservation> evaluateNow() {
        return runAnalysisCycle();
    }

    private synchronized List<IncidentObservation> runAnalysisCycle() {
        long startTime = System.currentTimeMillis();
        List<IncidentObservation> generatedObservations = new ArrayList<>();

        try {
            TelemetrySnapshot snapshot = telemetryRegistry.captureCombinedSnapshot();
            baselineEngine.recordSnapshot(snapshot);

            List<RuleEngine.RuleEvaluationPair> evaluations = ruleEngine.evaluateRules(snapshot, baselineEngine, health);

            Instant now = snapshot.getTimestamp();

            for (RuleEngine.RuleEvaluationPair pair : evaluations) {
                String pattern = pair.getRule().pattern();
                DetectionResult result = pair.getResult();

                Optional<IncidentObservation> obsOpt = lifecycleManager.processEvaluation(pattern, result, now);
                if (obsOpt.isPresent()) {
                    IncidentObservation obs = obsOpt.get();
                    generatedObservations.add(obs);
                    health.recordObservation();
                    publisherManager.publish(obs, health);
                }
            }
        } catch (Exception e) {
            log.error("Unhandled exception in IncidentDetector analysis cycle", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            health.recordEvaluation(duration);
        }

        return generatedObservations;
    }

    public void recordTelemetry(String metric, double value) {
        telemetryRegistry.recordMetric(metric, value);
    }

    public void recordTelemetry(String metric, double value, String unit, Map<String, String> tags) {
        telemetryRegistry.recordMetric(metric, value, unit, tags);
    }

    public void recordKeyDistribution(String component, Map<String, Double> distribution) {
        telemetryRegistry.recordKeyDistribution(component, distribution);
    }

    public void registerRule(IncidentRule rule) {
        ruleEngine.registerRule(rule);
    }

    public void registerProvider(TelemetryProvider provider) {
        telemetryRegistry.registerProvider(provider);
    }

    public void registerPublisher(ObservationPublisher publisher) {
        publisherManager.registerPublisher(publisher);
    }

    public DetectorHealth getHealth() {
        return health;
    }

    public Map<String, IncidentLifecycleManager.ActiveIncident> getActiveIncidents() {
        return lifecycleManager.getActiveIncidents();
    }

    public ObservationStore getObservationStore() {
        return observationStore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TelemetryRegistry telemetryRegistry = new TelemetryRegistry();
        private SlidingWindowBaselineEngine baselineEngine = new SlidingWindowBaselineEngine();
        private RuleEngine ruleEngine = new RuleEngine();
        private IncidentLifecycleManager lifecycleManager = new IncidentLifecycleManager();
        private PublisherManager publisherManager = new PublisherManager();
        private ObservationStore observationStore;
        private Duration evaluationInterval = Duration.ofSeconds(10);
        private boolean storeInPublisherManager = true;

        public Builder enableDefaults() {
            // Can be populated by caller or starters
            return this;
        }

        public Builder evaluationInterval(Duration interval) {
            this.evaluationInterval = interval;
            return this;
        }

        public Builder baselineWindow(Duration maxWindow) {
            this.baselineEngine = new SlidingWindowBaselineEngine(maxWindow, 1000);
            return this;
        }

        public Builder cooldownWindow(Duration cooldown) {
            this.lifecycleManager = new IncidentLifecycleManager(cooldown);
            return this;
        }

        public Builder registerRule(IncidentRule rule) {
            this.ruleEngine.registerRule(rule);
            return this;
        }

        public Builder registerProvider(TelemetryProvider provider) {
            this.telemetryRegistry.registerProvider(provider);
            return this;
        }

        public Builder registerPublisher(ObservationPublisher publisher) {
            this.publisherManager.registerPublisher(publisher);
            return this;
        }

        public Builder observationStore(ObservationStore store) {
            this.observationStore = store;
            return this;
        }

        public IncidentDetector build() {
            return new IncidentDetector(this);
        }
    }
}
