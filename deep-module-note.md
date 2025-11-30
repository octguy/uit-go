# High-level choices (what I recommend and why)

* **Asynchronous communication between TripService ↔ DriverService**: use a lightweight message broker (RabbitMQ / NATS / Kafka). (DONE: TripService send trip notification to Driver via RabbitMQ)

  * **Why**: absorbs traffic spikes, decouples services, prevents DriverService from being overwhelmed.
  * **Trade-off**: adds eventual consistency and slightly higher end-to-end latency for booking flow; increases operational complexity (broker, delivery guarantees).
  * **Local alternative**: run RabbitMQ or NATS as a k8s deployment (Helm charts).

* **Event-driven / CQRS pattern for driver search and location updates**: (DONT UNDERSTAND YET)

  * Write location updates as events to a stream; materialize search index (in Redis or in-memory store) for quick lookups.
  * **Why**: separates reads (fast, read-optimized) from writes (durable, ordered), improves search throughput.
  * **Trade-off**: complexity in synchronizing eventual consistent materialized view vs. strict consistency.

* **In-memory caching for read-heavy data**: Redis (Helm). (DONE: Driver location, Trip availability for driver)

  * Cache driver availability (NOT YET), geo-tiles, and static metadata.
  * **Trade-off**: stale reads unless TTLs or invalidation are used.

* **Database strategy**: (DONE: VN-TH Sharding)

  * Primary OLTP DB (Postgres or MySQL) for authoritative state; **Read replicas** for scale (simulate locally) and connection pooling (pgbouncer).
  * Consider **logical partitioning / sharding** by geographic region when hyper-scale is required.
  * **Trade-off**: replication brings read scalability but increases operational complexity and possible replication lag -> consistency vs availability trade.

* **Kubernetes native scaling & resilience**: (DONE: Rolling update, HPA, PDB)

  * **HPA (HorizontalPodAutoscaler)** using CPU/memory and custom metrics (Prometheus adapter).
  * **VPA (Vertical Pod Autoscaler)** for stateful components as needed.
  * **PodDisruptionBudgets**, **readiness/liveness probes**, and **resource requests/limits** to avoid noisy-neighbor effects.

* **Observability & SRE tooling (local)**:

  * Prometheus + Grafana for metrics, Jaeger for tracing, EFK (Elasticsearch/Fluentd/Kibana) or Loki + Grafana for logs.
  * Use Prometheus metrics to drive HPA and to validate load tests.

* **Traffic shaping & reliability patterns**:

  * Circuit breaker (e.g., via application libs or service mesh), bulkheads, retries with exponential backoff, rate limiting at API gateway or ingress (NGINX/Envoy).

* **Service mesh (optional for local)**:

  * Linkerd or Istio for advanced routing, mTLS, slow-down testing, and traffic-shifting experiments. Adds complexity; use only if you need cross-service observability and resilience features.

---

# Local replacements for cloud services

| Cloud service          | Local replacement (k8s)                                                                            |
| ---------------------- | -------------------------------------------------------------------------------------------------- |
| SQS                    | RabbitMQ / NATS / Kafka (Helm)                                                                     |
| Elasticache (Redis)    | Redis Helm chart                                                                                   |
| Auto Scaling Group     | k8s HPA + Cluster autoscaler (if using multi-node local cluster like kind/ k3d, you can add nodes) |
| RDS read replicas      | Postgres primary + replicas via Patroni or simple dockerized replicas (StatefulSet)                |
| Cloud IAM / managed LB | MetalLB + k8s RBAC for local LB & networking                                                       |

---

# Concrete design decisions to **defend** (with short defensible statements you can include in the report)

1. **Use asynchronous queue between TripService and DriverService**

   * *Defense*: “Queues decouple spike handling; they protect DriverService from overload at the cost of slightly higher search/assignment latency. For peak surge scenarios (e.g., flash promos), availability is prioritized over minimal sub-100ms assignment latency.”
2. **Materialized geo-index in Redis for driver search**

   * *Defense*: “Maintains low-latency lookup for nearest drivers; eventual consistency acceptable for short-lived location updates because frequent updates are preserved in the event stream.”
3. **Read replicas + connection pooling**

   * *Defense*: “Offloads heavy read traffic (user dashboards, analytics) from primary write DB, improving write latency and transactional throughput while accepting potential replication lag for non-critical reads.”
4. **HPA based on custom queue length metric**

   * *Defense*: “Scaling by CPU only reacts late. Using queue backlog (or request latency) as a metric matches application load more closely.”

---

# Load testing plan (how to **prove** the design)

**Tools**: k6 (recommended), Locust or JMeter as alternatives. Use Prometheus + Grafana to capture system metrics during tests.

## Scenarios to build

1. **Driver search steady state**: baseline RPS (normal traffic) — measure p95/p99 latency and success rate.
2. **Driver search sudden spike**: 10–100× ramp in seconds — verify broker absorbs spike and DriverService remains stable.
3. **Location update flood**: simulate thousands of drivers pushing location updates per second — validate processing pipeline (ingestion → materialized store).
4. **Combined scenario**: search + booking + location updates mixed to test end-to-end effects.

## Metrics to capture

* Application: request latency (p50/p95/p99), error rates, queue length, DB query latencies.
* Infra: pod CPU/memory, node CPU/memory, disk I/O, network usage.
* Business: % successful bookings within SLA.

## How to run locally

1. Deploy services in k8s with proper resource requests/limits and readiness probes.
2. Deploy Prometheus & Grafana (Helm). Configure dashboards for the metrics above.
3. Run k6 test from a local pod (create a `k6` job in cluster so load originates internal) to avoid local machine bottleneck.
4. Gradually increase load; record thresholds at which errors or latencies exceed targets.

## Example k6 scenario (pseudocode)

```js
import http from 'k6/http';
import { sleep } from 'k6';
export let options = { stages: [{duration: '2m', target: 100}, {duration: '5m', target: 1000}] };
export default function () {
  // driver search endpoint
  http.get('http://tripservice.internal/api/v1/search?lat=...&lon=...');
  sleep(1);
}
```

(Use k6 checks and thresholds to fail the test if latency > SLA.)

---

# Tuning & hardening actions (what to implement after baseline tests)

1. **Caching**

   * Redis for geo tile cache, TTLs, and invalidation on event updates.
2. **Connection pooling**

   * pgbouncer for Postgres or DB connection pool settings tuned per pod.
3. **Indexes & query tuning**

   * Add spatial indexes (PostGIS) or GIN indexes where appropriate.
4. **Partitioning/sharding**

   * Logical partition by region or city for hot-spot mitigation.
5. **Autoscaling adjustments**

   * HPA via custom metrics: queue length, request latency or external metrics.
6. **Backpressure & throttling**

   * Limit client request rate and protect internal services (bulkheads).
7. **Circuit breakers and retries**

   * Implement per-service circuit breakers (Resilience4j, Istio policies) & exponential backoff for retries.
8. **Resource limits**

   * Right-size CPU/memory for each service and create bursting allowances.
9. **Use read replicas for analytics & dashboards**

   * Route analytics & non-critical reads to replicas.

---

# Observability & experiment tracking

* **Tracing**: Jaeger for distributed traces; ensure driver search flow is traced end-to-end.
* **Metrics**: Expose Prometheus metrics from services; record during every test run.
* **Logging**: Centralize logs (EFK or Loki) and attach log-correlation IDs to traces.
* **Test result storage**: store k6 results (JSON) and ingest into Grafana or export charts for the report.

---

# Deliverables (what you will produce)

1. **Design report** (core deliverable)

   * Architecture diagrams (component, sequence for search & booking).
   * Design decisions & trade-offs (Consistency vs Availability, Cost vs Performance, Complexity vs Speed).
   * Chosen tech stack and justification (local k8s equivalents).
2. **k8s manifests / Helm values** to reproduce cluster: service deployments, RabbitMQ, Redis, Postgres + replicas, Prometheus/Grafana, Jaeger.
3. **Load testing artifacts**:

   * k6 scripts, run commands, and raw results.
   * Grafana dashboards snapshots, Prometheus query list.
4. **Tuning actions & changed configs** (before/after): caching enabled, connection pool config, indexes added.
5. **Results section**:

   * Side-by-side charts of pre/tuned metrics (RPS, p95/p99 latencies, error rates).
   * Bottleneck analysis and recommendations for next steps (e.g., shard DB by city).
6. **Runbook for local dev / SRE**:

   * How to scale services, add replicas, read metrics to make decisions, and revert changes.

---

# Suggested technology list (concise)

* Message broker: **RabbitMQ** or **NATS** (Helm)
* Cache: **Redis** (Helm)
* DB: **Postgres** (+ Patroni or simulated replicas), **pgbouncer**
* Load test: **k6** (run as k8s job), alternative Locust/JMeter
* Metrics & scaling: **Prometheus**, **Grafana**, **k8s HPA** with Prometheus adapter, **metrics-server** for CPU
* Tracing: **Jaeger**
* Logging: **EFK** or **Loki + Grafana**
* Optional: **Linkerd/ Istio** (if you want service mesh features)
* Local k8s: **kind**, **k3d**, or **microk8s** (depending on how you run cluster)

---

# Suggested step-by-step plan (2–4 week sprint style; local execution)

**Phase 0 — Prep (1–2 days)**

* Confirm local k8s tool (kind/k3d/microk8s). Install Helm, kubectl, metrics-server.
* Create namespaces, RBAC, and basic app manifests.

**Phase 1 — Baseline deploy (2–3 days)**

* Deploy TripService, DriverService, DB (single primary), Redis (but not used yet), RabbitMQ (but not wired yet).
* Deploy Prometheus & Grafana, Jaeger, and a basic dashboard.
* Smoke test and capture baseline.

**Phase 2 — Baseline load testing (2–3 days)**

* Create k6 scenarios (search, location update, spike). Run baseline tests and gather metrics.

**Phase 3 — Implement architecture changes (3–5 days)**

* Introduce RabbitMQ between TripService and DriverService; implement consumer logic.
* Add Redis materialized store for driver search.
* Add DB read replica simulation & pgbouncer.
* Add HPA (CPU initially) and later HPA via custom metrics (queue length).

**Phase 4 — Verify & tune (3–5 days)**

* Re-run load tests, measure improvements, tune caching TTLs, DB indexes, pooling, and HPA thresholds.
* Iterate until performance targets met or diminishing returns.

**Phase 5 — Final report & runbook (1–2 days)**

* Compile results, graphs, design justification, trade-offs, and runbook.

---

# Example short checklist for final report section “Design choices & trade-offs”

* Why asynchronous queue? (Availability > latency under surge)
* Why Redis materialized view? (Predictable low latency reads vs eventual consistency)
* Why read replicas? (Scale read throughput; trade replication lag)
* Why HPA on custom metric? (Faster reaction to backlog than CPU only)
* Cost of complexity (team ops burden vs huge scale capability)

---

# Quick notes for local constraints & realism

* Local k8s cannot perfectly emulate cloud autoscaling or large-node clusters; use synthetic load and metric thresholds to simulate behavior and identify software bottlenecks.
* When documenting numbers, present them as “observed on local cluster X nodes” and avoid extrapolating to cloud without further testing.

---

If you want I can next:

* produce a minimal set of **k8s manifests + Helm values** for TripService + RabbitMQ + Redis + Postgres + Prometheus so you can deploy baseline locally, **and**
* write the **first k6 scripts** (driver search + spike) ready to run as a k8s job.

Tell me which one you want me to deliver first (manifests or k6 scripts) and I’ll produce them right away.
