# Terraform + Local Kubernetes PoC Plan - UIT-Go Project

## Overview
Deploy your UIT-Go microservices using Terraform to manage local Kubernetes infrastructure. This gives you container orchestration experience while staying local.

## Local Kubernetes Options for Single Laptop

### Option 1: Minikube (Recommended for Beginners)
- **Resource Usage**: Light (2-4GB RAM)
- **Features**: Full Kubernetes API, addons, dashboard
- **Best For**: Learning Kubernetes concepts

### Option 2: Docker Desktop Kubernetes (I choose this)
- **Resource Usage**: Medium (4-6GB RAM)
- **Features**: Built into Docker Desktop, easy setup
- **Best For**: Windows/Mac users with Docker Desktop

### Option 3: Kind (Kubernetes in Docker)
- **Resource Usage**: Light (1-3GB RAM)
- **Features**: Multiple node clusters, CI/CD friendly
- **Best For**: Advanced users, testing

### Option 4: K3s (Lightweight Kubernetes)
- **Resource Usage**: Very Light (512MB-2GB RAM)
- **Features**: Production-ready, IoT optimized
- **Best For**: Resource-constrained environments

## Recommended Architecture: Terraform + Minikube + UIT-Go

### Project Structure
```
terraform/
├── main.tf                    # Main configuration
├── providers.tf              # Kubernetes & Helm providers
├── variables.tf              # Input variables
├── outputs.tf               # Output values
├── kubernetes/
│   ├── namespaces.tf         # Kubernetes namespaces
│   ├── configmaps.tf         # Configuration management
│   ├── secrets.tf            # Sensitive data
│   ├── databases.tf          # PostgreSQL deployments
│   ├── services.tf           # Spring Boot services
│   ├── grpc-services.tf      # Go gRPC services
│   ├── ingress.tf            # API Gateway/Ingress
│   └── monitoring.tf         # Prometheus/Grafana
└── helm/
    ├── postgresql.tf         # PostgreSQL Helm chart
    └── monitoring.tf         # Monitoring stack
```

## Phase 1: Setup Local Kubernetes

### 1.1 Install Prerequisites
```bash
# Install Minikube
choco install minikube

# Install kubectl
choco install kubernetes-cli

# Install Helm (for package management)
choco install kubernetes-helm

# Verify installations
minikube version
kubectl version --client
helm version
```

### 1.2 Start Minikube
```bash
# Start with sufficient resources
minikube start --memory=8192 --cpus=4 --disk-size=50g

# Enable addons
minikube addons enable dashboard
minikube addons enable ingress
minikube addons enable metrics-server

# Verify cluster
kubectl cluster-info
kubectl get nodes
```

## Phase 2: Terraform Configuration

### 2.1 Providers (`providers.tf`)
```hcl
terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  required_version = ">= 1.0"
}

provider "kubernetes" {
  config_path = "~/.kube/config"
  config_context = "minikube"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
    config_context = "minikube"
  }
}
```

### 2.2 Variables (`variables.tf`)
```hcl
variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "uit-go"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "namespace" {
  description = "Kubernetes namespace"
  type        = string
  default     = "uit-go-dev"
}

variable "replicas" {
  description = "Number of replicas for each service"
  type        = map(number)
  default = {
    user_service   = 1
    trip_service   = 1
    driver_service = 1
    user_grpc      = 1
    trip_grpc      = 1
    driver_grpc    = 1
  }
}
```

### 2.3 Namespaces (`kubernetes/namespaces.tf`)
```hcl
resource "kubernetes_namespace" "uit_go" {
  metadata {
    name = var.namespace
    labels = {
      name = var.namespace
      environment = var.environment
    }
  }
}

resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = "${var.namespace}-monitoring"
    labels = {
      name = "monitoring"
      environment = var.environment
    }
  }
}
```

### 2.4 PostgreSQL Database (`kubernetes/databases.tf`)
```hcl
# User Service Database
resource "kubernetes_deployment" "user_db" {
  metadata {
    name      = "user-db"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
    labels = {
      app = "user-db"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "user-db"
      }
    }

    template {
      metadata {
        labels = {
          app = "user-db"
        }
      }

      spec {
        container {
          image = "postgres:15"
          name  = "postgres"

          env {
            name  = "POSTGRES_DB"
            value = "user_service_db"
          }
          env {
            name  = "POSTGRES_USER"
            value = "user_service_user"
          }
          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.db_passwords.metadata[0].name
                key  = "user_db_password"
              }
            }
          }

          port {
            container_port = 5432
          }

          volume_mount {
            name       = "user-db-storage"
            mount_path = "/var/lib/postgresql/data"
          }
        }

        volume {
          name = "user-db-storage"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.user_db.metadata[0].name
          }
        }
      }
    }
  }
}

# Database Service
resource "kubernetes_service" "user_db" {
  metadata {
    name      = "user-db"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "user-db"
    }

    port {
      port        = 5432
      target_port = 5432
    }

    type = "ClusterIP"
  }
}

# Persistent Volume Claim
resource "kubernetes_persistent_volume_claim" "user_db" {
  metadata {
    name      = "user-db-pvc"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "5Gi"
      }
    }
  }
}
```

### 2.5 Secrets (`kubernetes/secrets.tf`)
```hcl
resource "kubernetes_secret" "db_passwords" {
  metadata {
    name      = "db-passwords"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  data = {
    user_db_password   = base64encode("user_password123")
    trip_db_password   = base64encode("trip_password123")
    driver_db_password = base64encode("driver_password123")
  }

  type = "Opaque"
}
```

### 2.6 Spring Boot Services (`kubernetes/services.tf`)
```hcl
# User Service Deployment
resource "kubernetes_deployment" "user_service" {
  metadata {
    name      = "user-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
    labels = {
      app = "user-service"
    }
  }

  spec {
    replicas = var.replicas.user_service

    selector {
      match_labels = {
        app = "user-service"
      }
    }

    template {
      metadata {
        labels = {
          app = "user-service"
        }
      }

      spec {
        container {
          image = "uit-go/user-service:latest"
          name  = "user-service"

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.environment
          }
          env {
            name  = "DB_HOST"
            value = "user-db"
          }
          env {
            name = "DB_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.db_passwords.metadata[0].name
                key  = "user_db_password"
              }
            }
          }

          port {
            container_port = 8081
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8081
            }
            initial_delay_seconds = 60
            period_seconds        = 30
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8081
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }

          resources {
            requests = {
              memory = "512Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.user_db]
}

# User Service Kubernetes Service
resource "kubernetes_service" "user_service" {
  metadata {
    name      = "user-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "user-service"
    }

    port {
      port        = 8081
      target_port = 8081
    }

    type = "ClusterIP"
  }
}
```

### 2.7 Ingress (`kubernetes/ingress.tf`)
```hcl
resource "kubernetes_ingress_v1" "uit_go_ingress" {
  metadata {
    name      = "uit-go-ingress"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class" = "nginx"
    }
  }

  spec {
    rule {
      host = "uit-go.local"
      http {
        path {
          backend {
            service {
              name = kubernetes_service.user_service.metadata[0].name
              port {
                number = 8081
              }
            }
          }
          path      = "/api/users"
          path_type = "Prefix"
        }

        path {
          backend {
            service {
              name = kubernetes_service.trip_service.metadata[0].name
              port {
                number = 8082
              }
            }
          }
          path      = "/api/trips"
          path_type = "Prefix"
        }
      }
    }
  }
}
```

## Phase 3: Implementation Steps

### 3.1 Setup Minikube
```bash
# 1. Start Minikube
minikube start --memory=8192 --cpus=4

# 2. Verify cluster
kubectl get nodes

# 3. Enable Docker environment (to build images locally)
eval $(minikube docker-env)
```

### 3.2 Build Images in Minikube
```bash
# Build your Spring Boot images in Minikube's Docker
docker build -t uit-go/user-service:latest ./backend/user-service
docker build -t uit-go/trip-service:latest ./backend/trip-service
docker build -t uit-go/driver-service:latest ./backend/driver-service
```

### 3.3 Deploy with Terraform
```bash
# 1. Initialize Terraform
cd terraform
terraform init

# 2. Plan deployment
terraform plan

# 3. Deploy to Minikube
terraform apply

# 4. Check deployments
kubectl get pods -n uit-go-dev
kubectl get services -n uit-go-dev
```

### 3.4 Access Services
```bash
# Port forward to access services
kubectl port-forward service/user-service 8081:8081 -n uit-go-dev

# Or use Minikube tunnel for ingress
minikube tunnel

# Access via ingress (add to hosts file)
echo "$(minikube ip) uit-go.local" >> /etc/hosts
```

## Phase 4: Benefits of This Approach

### 4.1 Learning Benefits
✅ **Kubernetes Concepts**: Pods, Services, Deployments, Ingress  
✅ **Container Orchestration**: Scaling, health checks, rolling updates  
✅ **Infrastructure as Code**: Terraform + Kubernetes  
✅ **Cloud Native Patterns**: Microservices, service discovery  

### 4.2 Production Readiness
✅ **Scalable**: Easy to scale replicas  
✅ **Resilient**: Health checks and auto-restart  
✅ **Portable**: Same config works on EKS/GKE/AKS  
✅ **Observable**: Built-in monitoring capabilities  

## Phase 5: Resource Requirements

### Minimum Laptop Specs
- **RAM**: 8GB (12GB+ recommended)
- **CPU**: 4 cores
- **Disk**: 20GB free space
- **OS**: Windows 10/11, macOS, Linux

### Minikube Configuration
```bash
# Optimized for development
minikube start \
  --memory=6144 \
  --cpus=3 \
  --disk-size=30g \
  --kubernetes-version=v1.28.0
```

## Phase 6: Next Steps After Local Success

1. **EKS Migration**: Move to AWS EKS using same Terraform configs
2. **Helm Charts**: Convert to Helm charts for better packaging
3. **GitOps**: Implement ArgoCD or Flux for deployment
4. **Service Mesh**: Add Istio for advanced networking
5. **Observability**: Full monitoring stack with Prometheus/Grafana

## Getting Started Commands

```bash
# 1. Install tools
choco install minikube kubernetes-cli kubernetes-helm terraform

# 2. Start Minikube
minikube start --memory=8192 --cpus=4

# 3. Create terraform directory
mkdir terraform
cd terraform

# 4. Create the Terraform files (from above examples)

# 5. Deploy
terraform init
terraform plan
terraform apply

# 6. Verify
kubectl get all -n uit-go-dev
```

This approach gives you real Kubernetes experience while staying local, and the Terraform configurations will transfer directly to cloud providers later!