# Rule Catalog - Incident Pattern Detector

The library includes 10 initial production-grade failure pattern detectors. Each rule uses multi-signal evidence aggregation and confidence scoring to eliminate false positives.

---

## 1. POOL_SATURATION
- **Target**: Database, Redis, or HTTP connection pools.
- **Signals**: Pool utilization (> 90%), pending connection requests (> 5), acquire latency p95 (> 3x baseline), request latency p95.
- **Confidence Scoring**: Multi-signal score based on utilization, pending queue depth, acquire latency, and request latency escalation.
- **Explainability**: Identifies connection contention, potential connection leaks, or undersized connection pools.

---

## 2. RETRY_STORM
- **Target**: HTTP / gRPC client connections and downstream dependencies.
- **Signals**: Client retry ratio (> 20%), retry rate multiplier (> 4x baseline), dependency error rate (> 10%), request latency.
- **Confidence Scoring**: High confidence when dependency error rate spikes alongside sudden multiplication of retry requests.
- **Explainability**: Highlights retry amplification loops, lack of exponential backoff/jitter, and downstream service overload.

---

## 3. TIMEOUT_CASCADE
- **Target**: Multi-tier microservice architectures.
- **Signals**: Downstream dependency timeouts (> 5), upstream API timeouts (> 3), thread pool active thread accumulation, request latency.
- **Explainability**: Identifies temporal propagation of timeouts across service boundaries.

---

## 4. HOT_KEY
- **Target**: Redis, Memcached, or in-memory caches.
- **Signals**: Traffic concentration on top key (> 35% of total traffic rate).
- **Privacy Enforcement**: Enforces automatic SHA-256 key hashing (`sha256:a1b2c3d4e5f6`) to prevent leaking raw user tokens or PII into metrics and logs.

---

## 5. CACHE_THRASHING
- **Target**: Caching layer (Caffeine, Guava, Redis).
- **Signals**: Cache hit ratio drop (< 50%), eviction rate spike (> 4x baseline), backend database read surge.
- **Explainability**: Detects working set sizing issues, short TTLs, and cache stampedes bypassing the cache layer.

---

## 6. THREAD_POOL_STARVATION
- **Target**: Java `ExecutorService` and web server thread pools.
- **Signals**: Active threads (== maxThreads), queue depth (> 20), task wait time (> 100ms), rejected executions.
- **Guard Clause**: `activeThreads == maxThreads` ALONE is NOT classified as an incident. Multiple supporting signals (queue depth, wait time, or rejections) are strictly required.

---

## 7. CONSUMER_LAG
- **Target**: Kafka / MQ event consumers.
- **Signals**: Consumer lag (> 500 records), lag growth rate (> +10 rec/s), processing latency (> 300ms), partition imbalance.
- **Explainability**: Identifies slow message processing, partition key skew, or unaligned poll timeouts.

---

## 8. LARGE_PAYLOAD
- **Target**: HTTP / REST / gRPC API payloads.
- **Signals**: Payload size p95 (> 2 MB), serialization latency (> 80ms), JVM memory allocation rate.
- **Privacy Enforcement**: Analyzes byte metadata only; raw payload request/response bodies are **never** captured or stored.

---

## 9. GC_LATENCY
- **Target**: JVM Garbage Collectors (G1GC, ZGC, ParallelGC).
- **Signals**: STW GC pause duration (> 200ms), Full GC event count (> 0), heap utilization (> 85%), request latency.
- **Guard Clause**: GC events alone do not trigger an incident unless correlated with application latency spikes or STW pauses.

---

## 10. DEPENDENCY_DEGRADATION
- **Target**: External HTTP services, gRPC endpoints, databases, Redis.
- **Signals**: Dependency latency p95 (> 3x baseline), error rate (> 5%), timeout rate (> 3%).
- **Explainability**: Groups degradation by target service, host, endpoint, or database component.
