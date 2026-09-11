package io.github.incidentdetector.rules;

import io.github.incidentdetector.api.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Detects hot key access concentration in Redis/Cache systems while enforcing SHA-256 masking for raw keys.
 */
public class HotKeyRule implements IncidentRule {

    private final double hotKeyRatioThreshold;

    public HotKeyRule(double hotKeyRatioThreshold) {
        this.hotKeyRatioThreshold = hotKeyRatioThreshold;
    }

    public HotKeyRule() {
        this(0.35); // 35% of total traffic on a single key
    }

    @Override
    public String pattern() {
        return "HOT_KEY";
    }

    @Override
    public DetectionResult evaluate(TelemetrySnapshot snapshot, Baseline baseline) {
        Map<String, Double> redisDist = snapshot.getKeyDistribution("redis");
        if (redisDist == null || redisDist.isEmpty()) {
            redisDist = snapshot.getKeyDistribution("cache");
        }

        if (redisDist == null || redisDist.isEmpty()) {
            return DetectionResult.notTriggered();
        }

        double totalOps = 0.0;
        String topKeyRaw = null;
        double topKeyOps = 0.0;

        for (Map.Entry<String, Double> entry : redisDist.entrySet()) {
            double ops = entry.getValue();
            totalOps += ops;
            if (ops > topKeyOps) {
                topKeyOps = ops;
                topKeyRaw = entry.getKey();
            }
        }

        if (totalOps <= 0 || topKeyRaw == null) {
            return DetectionResult.notTriggered();
        }

        double ratio = topKeyOps / totalOps;

        if (ratio < hotKeyRatioThreshold) {
            return DetectionResult.notTriggered();
        }

        String maskedKey = hashOrMaskKey(topKeyRaw);

        List<Evidence> evidence = new ArrayList<>();
        double score = 0.40;

        evidence.add(new Evidence("hotkey.traffic_share", String.format("%.1f%%", ratio * 100),
                "5%", String.format("%.0f%%", hotKeyRatioThreshold * 100), "%", "ELEVATED", Duration.ofSeconds(10), 0.50));

        evidence.add(new Evidence("hotkey.access_rate", String.format("%.0fops", topKeyOps),
                "100ops", "1000ops", "ops", "SPIKE", Duration.ofSeconds(10), 0.30));

        evidence.add(new Evidence("hotkey.hashed_identifier", maskedKey,
                "N/A", "N/A", "", "IDENTIFIED", Duration.ofSeconds(10), 0.20));

        score += (ratio - hotKeyRatioThreshold) * 1.5;

        Severity severity = ratio >= 0.60 ? Severity.CRITICAL : (ratio >= 0.45 ? Severity.HIGH : Severity.MEDIUM);

        List<String> probableCauses = List.of(
                "Unbounded access concentration on a single cache key (e.g., viral content, global configuration, un-partitioned counter).",
                "Lack of key salting or local in-memory L1 caching for high-frequency keys."
        );

        List<String> potentialImpacts = List.of(
                "Redis node CPU saturation on single-threaded execution thread.",
                "Network bandwidth saturation on single Redis shard.",
                "Increased latency for all operations sharing the affected Redis shard."
        );

        List<String> recommendedActions = List.of(
                "Implement local near-cache (Caffeine/Guava L1 cache) for key " + maskedKey + ".",
                "Scatter hot key across multiple sub-keys using key salting (key:1, key:2).",
                "Audit application access patterns for un-cached hot keys."
        );

        return DetectionResult.builder()
                .triggered(true)
                .confidence(Math.min(0.99, score))
                .severity(severity)
                .summary("Hot key access concentration detected: single key receiving " + String.format("%.1f%%", ratio * 100) + " of total traffic.")
                .evidence(evidence)
                .probableCauses(probableCauses)
                .potentialImpacts(potentialImpacts)
                .recommendedActions(recommendedActions)
                .dimensions(Map.of("component", "redis", "key_hash", maskedKey))
                .build();
    }

    private String hashOrMaskKey(String key) {
        if (key == null) return "unknown";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            return "sha256:" + hex.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return "masked_key_" + Math.abs(key.hashCode());
        }
    }
}
