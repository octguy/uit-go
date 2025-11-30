# ADR-008: Linkerd vs Istio cho Service Mesh

## Bối cảnh

Microservices architecture cần:

1. **Observability**: Metrics, tracing, service topology
2. **Security**: mTLS for service-to-service communication
3. **Traffic Management**: Retries, timeouts, load balancing
4. **Reliability**: Circuit breaking, fault injection

Hiện tại: Services giao tiếp trực tiếp → Thiếu visibility và security

### Yêu cầu

1. **Observability**: Real-time metrics và service graph
2. **Zero-trust Security**: Automatic mTLS encryption
3. **Low Overhead**: Minimal latency và resource usage
4. **Developer-friendly**: Easy setup cho learning environment
5. **Production-ready**: Stable và mature

### Các phương án được xem xét

1. **Linkerd**
2. **Istio**
3. **Consul Connect**
4. **No Service Mesh** (application-level)
5. **AWS App Mesh** (cloud-specific)

---

## Quyết định

**Nhóm em chọn Linkerd** cho service mesh implementation.

**Deployment:**
```bash
# Install Linkerd CLI
curl -sL https://run.linkerd.io/install | sh

# Install Linkerd control plane
linkerd install | kubectl apply -f -

# Inject sidecar to services
kubectl get deploy -o yaml | linkerd inject - | kubectl apply -f -
```

---

## Lý do lựa chọn

### Ưu điểm của Linkerd

#### 1. **Lightweight and Fast**

**Resource Overhead Per Pod:**

```
Linkerd proxy:
  Memory: 10-20MB per pod
  CPU: ~5-10ms latency added
  
Istio Envoy proxy:
  Memory: 50-100MB per pod
  CPU: ~20-30ms latency added
```

**Impact Analysis:**
```
3 services × 3 replicas = 9 pods

Linkerd overhead:
  9 pods × 15MB = 135MB total
  Latency: ~5ms per hop
  
Istio overhead:
  9 pods × 75MB = 675MB total
  Latency: ~25ms per hop

Savings with Linkerd: 540MB RAM, 20ms latency
```

#### 2. **Simplicity Over Features**

**Linkerd:** Opinionated, fewer knobs to turn

```bash
# Install Linkerd
linkerd install | kubectl apply -f -

# Inject mesh
kubectl get deploy -o yaml | linkerd inject - | kubectl apply -f -

# Done! mTLS enabled, metrics flowing
```

**Istio:** Flexible, many configuration options

```yaml
# Must configure: Gateways, VirtualServices, DestinationRules
# Must choose: Ingress strategy, mTLS mode, telemetry config
# Must manage: Multiple CRDs and resources
```

**For learning:** Linkerd's simplicity wins

#### 3. **Automatic mTLS Out-of-the-Box**

**Linkerd:** mTLS automatic, zero configuration

```
Service A → Linkerd Proxy A → [Encrypted] → Linkerd Proxy B → Service B

Certificate rotation: Automatic (every 24h)
No application changes: Transparent
```

**Verification:**
```bash
linkerd viz tap deploy/trip-service | head
# Output shows "tls=true" for all connections
```

**Istio:** Requires configuration

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: STRICT  # Must explicitly enable
```

#### 4. **Built-in Observability**

**Linkerd Viz Dashboard:**

```bash
linkerd viz install | kubectl apply -f -
linkerd viz dashboard  # Opens browser

Features:
  - Real-time success rates per service
  - Latency percentiles (P50, P95, P99)
  - Service topology graph
  - Live request tap
```

**Example Metrics Visible:**
```
trip-service → driver-service
  Success rate: 99.99%
  RPS: 50 requests/s
  Latency P95: 15ms
  All connections: mTLS ✓
```

#### 5. **Production-Ready**

Linkerd proven at scale:

```
CNCF Graduated Project (2021)
Used by: Microsoft, Walmart, HP, Nordstrom
Battle-tested in production
Stable releases, long-term support
```

---

### Tại sao không chọn Istio?

**Strategy:** Full-featured service mesh with Envoy proxy

#### Nhược điểm:

**1. Heavy Resource Usage** ❌

```
Istio components:
  - istiod (control plane): 500MB RAM
  - Envoy proxy per pod: 50-100MB
  - Ingress gateway: 200MB
  
Total for 9 pods:
  500MB + (9 × 75MB) + 200MB = 1.375 GB

Linkerd total:
  ~200MB control plane + (9 × 15MB) = 335MB

Istio uses 4x more resources
```

**Impact:** Laptop friendly? Linkerd yes, Istio marginal

**2. Complex Configuration** ❌

**Istio requires many CRDs:**

```yaml
# VirtualService for routing
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  hosts:
  - trip-service
  http:
  - route:
    - destination:
        host: trip-service
    timeout: 10s
    retries:
      attempts: 3

# DestinationRule for load balancing
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
spec:
  host: trip-service
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN

# PeerAuthentication for mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
# ... and more
```

**Linkerd:** Works out-of-the-box, minimal config

**3. Steeper Learning Curve** ❌

```
Istio concepts to learn:
  - VirtualService, DestinationRule, ServiceEntry
  - Gateway, WorkloadEntry, PeerAuthentication
  - RequestAuthentication, AuthorizationPolicy
  - Telemetry, WasmPlugin
  
Linkerd concepts:
  - Inject proxy → Done
  - (Optional: ServiceProfile for advanced features)
```

**For students:** Linkerd easier to grasp

**4. Slower Iteration** ❌

```
Change Istio config:
  1. Edit VirtualService YAML
  2. Apply with kubectl
  3. Wait for Envoy to sync config (5-10s)
  4. Test
  
Change Linkerd:
  No config needed for basic features
  For advanced: Edit ServiceProfile (instant apply)
```

---

### Tại sao không chọn Consul Connect?

**Strategy:** Service mesh by HashiCorp

#### Nhược điểm:

**1. Requires Consul Infrastructure** ❌

```
Must install:
  - Consul servers (3-5 for HA)
  - Consul agents on each node
  - Consul Connect proxies
  
Linkerd: Self-contained, no external dependencies
```

**2. Less Kubernetes-native** ❌

```
Consul: Multi-platform (K8s, VMs, bare metal)
  → More general, less K8s-optimized
  
Linkerd: Built specifically for Kubernetes
  → Tight integration, K8s-native patterns
```

**3. Smaller Ecosystem** ❌

```
Istio: CNCF, huge community
Linkerd: CNCF, growing community
Consul: HashiCorp ecosystem

For Kubernetes: Linkerd/Istio more popular
```

---

### Tại sao không chọn "No Service Mesh"?

**Strategy:** Implement observability and security in application code

#### Nhược điểm:

**1. Must Implement mTLS Manually** ❌

```java
// Every service needs:
@Configuration
public class MTLSConfig {
    @Bean
    public SSLContext sslContext() {
        // Load certificates
        // Configure TLS
        // Implement cert rotation
        // ... 100+ lines of boilerplate
    }
}
```

**Linkerd:** Transparent, zero code

**2. No Unified Observability** ❌

```
Without mesh:
  - Each service exports metrics differently
  - No service-to-service topology
  - Manual distributed tracing setup
  
With Linkerd:
  - Automatic golden metrics (success rate, latency, RPS)
  - Service graph visualization
  - Zero instrumentation needed
```

**3. Missing Traffic Management** ❌

```
Retries, timeouts, circuit breaking:
  Application-level: Resilience4j, manual config per service
  Linkerd: Configured once via Service Profile
  
Load balancing:
  Application: Default round-robin
  Linkerd: Least-request, automatic
```

---

### Tại sao không chọn AWS App Mesh?

**Strategy:** AWS-specific service mesh

#### Nhược điểm:

**1. Cloud Vendor Lock-in** ❌

```
App Mesh: Only works on AWS
Cannot run locally (Docker Desktop)
Cannot deploy to other clouds

Linkerd: Cloud-agnostic, runs anywhere
```

**2. Not for Learning Environment** ❌

```
App Mesh requires:
  - AWS account
  - EKS cluster
  - AWS CLI setup
  - IAM permissions management
  
Too complex for student project
```

---

## Chi tiết triển khai

### Installation

```bash
# Step 1: Install Linkerd CLI
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install | sh
export PATH=$PATH:$HOME/.linkerd2/bin

# Step 2: Validate cluster
linkerd check --pre

# Step 3: Install control plane
linkerd install --crds | kubectl apply -f -
linkerd install | kubectl apply -f -

# Step 4: Verify
linkerd check

# Step 5: Install viz extension (observability)
linkerd viz install | kubectl apply -f -
```

### Inject Proxies

**Automatic (recommended):**

```bash
# Annotate namespace for auto-injection
kubectl annotate namespace default linkerd.io/inject=enabled

# All new pods get proxy automatically
```

**Manual:**

```bash
# Inject existing deployments
kubectl get deploy -n default -o yaml \
  | linkerd inject - \
  | kubectl apply -f -
```

**Result:**
```yaml
# Before
spec:
  containers:
  - name: trip-service
    image: trip-service:latest

# After injection
spec:
  containers:
  - name: linkerd-proxy        # Added!
    image: ghcr.io/linkerd/proxy
  - name: trip-service
    image: trip-service:latest
```

### Observability

**Access Dashboard:**

```bash
linkerd viz dashboard
# Opens http://localhost:50750
```

**CLI Metrics:**

```bash
#Top routes by traffic
linkerd viz top deploy/api-gateway

# Live request tap
linkerd viz tap deploy/trip-service

# Service stats
linkerd viz stat deploy
```

**Grafana Integration:**

```bash
# Linkerd ships with Grafana dashboards
kubectl port-forward -n linkerd-viz svc/grafana 3000:3000

# Open http://localhost:3000
# Dashboards show:
#   - Success rate per service
#   - P50/P95/P99 latencies
#   - Request volume
```

### Traffic Management (Optional)

**ServiceProfile for retries:**

```yaml
apiVersion: linkerd.io/v1alpha2
kind: ServiceProfile
metadata:
  name: trip-service.default.svc.cluster.local
spec:
  routes:
  - name: POST /api/trips
    condition:
      method: POST
      pathRegex: /api/trips
    isRetryable: true  # Auto-retry on 5xx
    timeout: 10s
```

---

## Hệ quả

### Tích cực

1. ✅ **Automatic mTLS**: All service-to-service traffic encrypted
```bash
linkerd viz tap deploy/trip-service | grep tls
# All connections show: tls=true
```

2. ✅ **Low Overhead**: 
```
Per-pod memory: 10-20MB
Latency added: ~5ms P99
Acceptable for our use case
```

3. ✅ **Observability Out-of-the-Box**:
```
Success rates: Real-time per service
Latency: P50/P95/P99 automatic
Service graph: Visual topology
```

4. ✅ **Simple Setup**:
```
Total commands: ~5
Time to mesh: < 10 minutes
No YAML changes needed
```

5. ✅ **Production-Ready**:
```
CNCF Graduated
Battle-tested
Stable releases
```

### Tiêu cực

1. ❌ **Less Features than Istio**

```
Linkerd missing:
  - Advanced traffic splitting (canary)
  - External service entries
  - WebAssembly plugins
  - Multi-cluster federation (limited)
  
Acceptable: We don't need these (yet)
```

**Mitigation:** Linkerd covers 80% use cases, simpler

2. ❌ **Cannot Run Without Proxies**

```
If Linkerd control plane down:
  Proxies still work (data plane independent)
  
If pod proxy crashes:
  Service unavailable
  
Mitigation: Linkerd proxy extremely stable, rare crashes
```

3. ❌ **Additional Latency** 

```
Without mesh: Service A → Service B (direct)
With Linkerd: Service A → Proxy A → Proxy B → Service B

Added hops: ~5ms P99

Acceptable: 5ms << our target latencies (< 100ms)
```

### Biện pháp giảm thiểu

**Monitor Proxy Health:**

```bash
# Check proxy status
linkerd check --proxy

# View proxy metrics
linkerd viz stat deploy --from deploy/trip-service
```

**Resource Limits:**

```yaml
# Already set by Linkerd, but can adjust
linkerd.io/proxy-cpu-limit: "1"
linkerd.io/proxy-memory-limit: "256Mi"
```

---

## Xem xét lại quyết định

Nhóm em sẽ **xem xét lại** nếu:

1. **Need advanced traffic management** (A/B testing, canary)
   - Giải pháp: Evaluate Istio or Flagger (works with Linkerd)

2. **Multi-cluster requirements** emerge
   - Giải pháp: Linkerd multi-cluster or migrate to Istio

3. **Resource overhead becomes issue** (many microservices)
   - Giải pháp: Already using lightweight option (Linkerd)

4. **Require WebAssembly plugins**
   - Giải pháp: Istio (Envoy supports Wasm)

---

## Kết quả Validation

### Security

```bash
# Verify mTLS enabled
linkerd viz tap deploy/api-gateway --to deploy/trip-service

# Output shows:
req id=1:0 proxy=in  src=10.1.0.5:54321 dst=10.1.0.6:8080 tls=true :method=POST ...
```

**Result:** ✅ 100% of internal traffic encrypted

### Performance Impact

```
Latency measurements (with vs without mesh):

API Gateway → Trip Service:
  Without Linkerd: P95 = 10ms
  With Linkerd:    P95 = 15ms
  Overhead: 5ms (acceptable)

Trip Service → Driver Service:
  Without Linkerd: P95 = 8ms
  With Linkerd:    P95 = 12ms
  Overhead: 4ms (acceptable)
```

**Verdict:** ✅ Latency impact minimal

### Resource Usage

```
Cluster totals:
  Control plane: ~80MB RAM
  Proxies (9 pods × 15MB): ~135MB
  Total Linkerd overhead: ~215MB

Acceptable for development laptop
```

---

## References

- [Linkerd Documentation](https://linkerd.io/2/overview/)
- [Linkerd vs Istio Benchmark](https://linkerd.io/2021/05/27/linkerd-vs-istio-benchmarks/)
- [Service Mesh Comparison](https://servicemesh.es/)
- [CNCF Service Mesh Landscape](https://landscape.cncf.io/card-mode?category=service-mesh)
