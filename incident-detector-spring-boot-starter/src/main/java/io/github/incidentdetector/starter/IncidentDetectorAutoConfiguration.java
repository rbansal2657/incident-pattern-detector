package io.github.incidentdetector.starter;

import io.github.incidentdetector.api.ObservationStore;
import io.github.incidentdetector.core.IncidentDetector;
import io.github.incidentdetector.rules.*;
import io.github.incidentdetector.storage.BoundedInMemoryObservationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(IncidentDetectorProperties.class)
@ConditionalOnProperty(name = "incident-detector.enabled", havingValue = "true", matchIfMissing = true)
public class IncidentDetectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObservationStore observationStore() {
        return new BoundedInMemoryObservationStore(1000, java.time.Duration.ofHours(24));
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public IncidentDetector incidentDetector(IncidentDetectorProperties properties, ObservationStore observationStore) {
        IncidentDetector.Builder builder = IncidentDetector.builder()
                .evaluationInterval(properties.getEvaluationInterval())
                .baselineWindow(properties.getBaselineWindow())
                .cooldownWindow(properties.getCooldownWindow())
                .observationStore(observationStore);

        // Register default 10 rules
        builder.registerRule(new PoolSaturationRule());
        builder.registerRule(new RetryStormRule());
        builder.registerRule(new TimeoutCascadeRule());
        builder.registerRule(new HotKeyRule());
        builder.registerRule(new CacheThrashingRule());
        builder.registerRule(new ThreadPoolStarvationRule());
        builder.registerRule(new ConsumerLagRule());
        builder.registerRule(new LargePayloadRule());
        builder.registerRule(new GcLatencyRule());
        builder.registerRule(new DependencyDegradationRule());

        IncidentDetector detector = builder.build();
        detector.start();
        return detector;
    }

    @Bean
    @ConditionalOnMissingBean
    public IncidentDetectorActuatorEndpoint incidentDetectorActuatorEndpoint(IncidentDetector detector, ObservationStore observationStore) {
        return new IncidentDetectorActuatorEndpoint(detector, observationStore);
    }
}
