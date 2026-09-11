package io.github.incidentdetector.core;

import io.github.incidentdetector.api.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CoreEngineTest {

    @Test
    public void testBaselineEngineCalculation() {
        SlidingWindowBaselineEngine engine = new SlidingWindowBaselineEngine(Duration.ofMinutes(5), 100);
        Instant now = Instant.now();

        engine.recordSnapshot(DefaultTelemetrySnapshot.builder().timestamp(now.minusSeconds(30)).putMetric("test.latency", 100).build());
        engine.recordSnapshot(DefaultTelemetrySnapshot.builder().timestamp(now.minusSeconds(20)).putMetric("test.latency", 200).build());
        engine.recordSnapshot(DefaultTelemetrySnapshot.builder().timestamp(now.minusSeconds(10)).putMetric("test.latency", 300).build());

        assertEquals(200.0, engine.getMovingAverage("test.latency", Duration.ofMinutes(1)));
        assertEquals(300.0, engine.getPercentile("test.latency", 99, Duration.ofMinutes(1)));
        assertEquals(2.0, engine.getRateOfChange("test.latency", Duration.ofMinutes(1)), 0.01);
    }

    @Test
    public void testLifecycleManagerTransitions() {
        IncidentLifecycleManager manager = new IncidentLifecycleManager(Duration.ofSeconds(10));
        Instant now = Instant.now();

        DetectionResult triggerRes = DetectionResult.builder()
                .triggered(true)
                .confidence(0.95)
                .severity(Severity.HIGH)
                .summary("Pool Saturated")
                .evidence(List.of(new Evidence("pool.utilization", 0.98, 0.40, 0.90, "%", "INCREASE", Duration.ofSeconds(10), 0.5)))
                .dimensions(Map.of("component", "redis"))
                .build();

        // 1. First trigger -> DETECTED
        Optional<IncidentObservation> opt1 = manager.processEvaluation("POOL_SATURATION", triggerRes, now);
        assertTrue(opt1.isPresent());
        assertEquals(IncidentState.DETECTED, opt1.get().getState());
        assertEquals("POOL_SATURATION", opt1.get().getPattern());

        // 2. Immediate second trigger -> Suppressed by cooldown
        Optional<IncidentObservation> opt2 = manager.processEvaluation("POOL_SATURATION", triggerRes, now.plusSeconds(2));
        assertFalse(opt2.isPresent());

        // 3. Condition cleared -> RESOLVED
        DetectionResult clearRes = DetectionResult.builder().triggered(false).confidence(0.0).build();
        Optional<IncidentObservation> opt3 = manager.processEvaluation("POOL_SATURATION", clearRes, now.plusSeconds(5));
        assertTrue(opt3.isPresent());
        assertEquals(IncidentState.RESOLVED, opt3.get().getState());
        assertTrue(opt3.get().getEvidence().size() > 0);
    }

    @Test
    public void testRuleAndPublisherFailureIsolation() {
        RuleEngine ruleEngine = new RuleEngine();
        
        // Faulty Rule
        ruleEngine.registerRule(new IncidentRule() {
            @Override
            public String pattern() {
                return "FAULTY_RULE";
            }

            @Override
            public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
                throw new RuntimeException("Simulated rule failure");
            }
        });

        // Good Rule
        ruleEngine.registerRule(new IncidentRule() {
            @Override
            public String pattern() {
                return "GOOD_RULE";
            }

            @Override
            public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
                return DetectionResult.builder().triggered(true).confidence(0.9).summary("All Good").build();
            }
        });

        DetectorHealth health = new DetectorHealth();
        List<RuleEngine.RuleEvaluationPair> results = ruleEngine.evaluateRules(DefaultTelemetrySnapshot.builder().build(), new SlidingWindowBaselineEngine(), health);

        assertEquals(1, results.size());
        assertEquals("GOOD_RULE", results.get(0).getRule().pattern());
        assertEquals(1, health.getRuleErrorsTotal());
    }
}
