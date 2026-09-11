# Prometheus & OpenTelemetry Integration Guide

## 1. Prometheus Metrics

The `incident-detector-prometheus` module exports machine-readable metrics via Micrometer or Prometheus Simple Client.

### Metric Names & Attributes

```text
# Active Incident Status Gauge (1 = Detected/Ongoing, 0 = Resolved)
incident_pattern_detected{pattern="POOL_SATURATION", component="redis", severity="CRITICAL"} 1

# Confidence Score Gauge (0.0 to 1.0)
incident_pattern_confidence{pattern="POOL_SATURATION", component="redis"} 0.94

# Total Incident Count Counter
incident_pattern_count_total{pattern="POOL_SATURATION", severity="CRITICAL"} 14

# Detector Health Metrics
incident_detector_evaluations_total 450
incident_detector_evaluation_duration 12.5
incident_detector_observations_total 8
incident_detector_rule_errors_total 0
incident_detector_queue_size 0
incident_detector_dropped_events_total 0
```

### Low Cardinality Rules
- Labels are sanitized (`a-z`, `0-9`, `_`).
- Sensitive data, raw keys, request IDs, user IDs, or un-sanitized URLs are strictly stripped.

---

## 2. OpenTelemetry Export

The `incident-detector-opentelemetry` module emits OpenTelemetry Log Events and Spans using standard semantic conventions:

- `incident.pattern`: Pattern string (e.g. `POOL_SATURATION`)
- `incident.severity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `incident.confidence`: Double precision confidence score (e.g. `0.94`)
- `incident.component`: Component name (e.g. `redis`, `http-client`, `jvm-gc`)
- `incident.state`: `DETECTED`, `ONGOING`, `RESOLVED`
