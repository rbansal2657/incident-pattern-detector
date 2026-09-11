# Contributing to Incident Pattern Detector

Thank you for considering contributing to **Incident Pattern Detector**!

## Guidelines

1. **Keep it Lightweight & Deterministic**:
   - Avoid adding external AI dependencies or network-blocking operations to the core evaluation path.
   - All rules must use multi-signal evidence and confidence scoring.

2. **Security & Privacy**:
   - Never capture raw payload bodies, user tokens, or PII in rules.
   - Ensure keys and sensitive strings are masked using SHA-256.

3. **Testing & Code Quality**:
   - Include unit tests for every new `IncidentRule`.
   - Test normal, incident, borderline, and recovery conditions.
   - Run `mvn clean test` before submitting PRs.
