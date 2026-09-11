package io.github.incidentdetector.core;

import io.github.incidentdetector.api.Baseline;
import io.github.incidentdetector.api.DetectionResult;
import io.github.incidentdetector.api.IncidentRule;
import io.github.incidentdetector.api.TelemetrySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Isolated Rule Engine that evaluates IncidentRules and protects the main loop against rule failures.
 */
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<IncidentRule> rules = new CopyOnWriteArrayList<>();

    public RuleEngine(List<IncidentRule> rules) {
        if (rules != null) {
            this.rules.addAll(rules);
        }
    }

    public RuleEngine() {
    }

    public void registerRule(IncidentRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    public void unregisterRule(String pattern) {
        rules.removeIf(r -> r.pattern().equalsIgnoreCase(pattern));
    }

    public List<RuleEvaluationPair> evaluateRules(TelemetrySnapshot snapshot, Baseline baseline, DetectorHealth health) {
        List<RuleEvaluationPair> results = new ArrayList<>();
        for (IncidentRule rule : rules) {
            if (!rule.isEnabled()) continue;
            try {
                DetectionResult res = rule.evaluate(snapshot, baseline);
                if (res != null) {
                    results.add(new RuleEvaluationPair(rule, res));
                }
            } catch (Exception e) {
                if (health != null) health.recordRuleError();
                log.error("Error executing rule [{}]", rule.pattern(), e);
            }
        }
        return results;
    }

    public List<IncidentRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public static class RuleEvaluationPair {
        private final IncidentRule rule;
        private final DetectionResult result;

        public RuleEvaluationPair(IncidentRule rule, DetectionResult result) {
            this.rule = rule;
            this.result = result;
        }

        public IncidentRule getRule() { return rule; }
        public DetectionResult getResult() { return result; }
    }
}
