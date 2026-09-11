package io.github.incidentdetector.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "incident-detector")
public class IncidentDetectorProperties {

    private boolean enabled = true;
    private Duration evaluationInterval = Duration.ofSeconds(10);
    private Duration baselineWindow = Duration.ofMinutes(15);
    private Duration cooldownWindow = Duration.ofSeconds(30);

    private Map<String, RuleConfig> rules = new HashMap<>();
    private PublishersConfig publishers = new PublishersConfig();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Duration getEvaluationInterval() { return evaluationInterval; }
    public void setEvaluationInterval(Duration evaluationInterval) { this.evaluationInterval = evaluationInterval; }

    public Duration getBaselineWindow() { return baselineWindow; }
    public void setBaselineWindow(Duration baselineWindow) { this.baselineWindow = baselineWindow; }

    public Duration getCooldownWindow() { return cooldownWindow; }
    public void setCooldownWindow(Duration cooldownWindow) { this.cooldownWindow = cooldownWindow; }

    public Map<String, RuleConfig> getRules() { return rules; }
    public void setRules(Map<String, RuleConfig> rules) { this.rules = rules; }

    public PublishersConfig getPublishers() { return publishers; }
    public void setPublishers(PublishersConfig publishers) { this.publishers = publishers; }

    public static class RuleConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class PublishersConfig {
        private PublisherToggle prometheus = new PublisherToggle(true);
        private PublisherToggle opentelemetry = new PublisherToggle(true);
        private PublisherToggle json = new PublisherToggle(true);
        private PublisherToggle storage = new PublisherToggle(true);

        public PublisherToggle getPrometheus() { return prometheus; }
        public void setPrometheus(PublisherToggle prometheus) { this.prometheus = prometheus; }

        public PublisherToggle getOpentelemetry() { return opentelemetry; }
        public void setOpentelemetry(PublisherToggle opentelemetry) { this.opentelemetry = opentelemetry; }

        public PublisherToggle getJson() { return json; }
        public void setJson(PublisherToggle json) { this.json = json; }

        public PublisherToggle getStorage() { return storage; }
        public void setStorage(PublisherToggle storage) { this.storage = storage; }
    }

    public static class PublisherToggle {
        private boolean enabled = true;

        public PublisherToggle() {}
        public PublisherToggle(boolean enabled) { this.enabled = enabled; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
