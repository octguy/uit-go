# k8s-manifests.tf - Kubernetes resources deployed via Terraform

# Kubernetes provider configuration
provider "kubernetes" {
  host                   = aws_eks_cluster.main.endpoint
  cluster_ca_certificate = base64decode(aws_eks_cluster.main.certificate_authority[0].data)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", aws_eks_cluster.main.name]
  }
}

provider "helm" {
  kubernetes {
    host                   = aws_eks_cluster.main.endpoint
    cluster_ca_certificate = base64decode(aws_eks_cluster.main.certificate_authority[0].data)

    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args        = ["eks", "get-token", "--cluster-name", aws_eks_cluster.main.name]
    }
  }
}

# Namespace for the application
resource "kubernetes_namespace" "uit_go" {
  metadata {
    name = "uit-go"
    labels = {
      name = "uit-go"
    }
  }

  depends_on = [aws_eks_node_group.main]
}

# AWS Load Balancer Controller using Helm
resource "helm_release" "aws_lb_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"
  version    = "1.6.2"

  set {
    name  = "clusterName"
    value = aws_eks_cluster.main.name
  }

  set {
    name  = "serviceAccount.create"
    value = "true"
  }

  set {
    name  = "serviceAccount.name"
    value = "aws-load-balancer-controller"
  }

  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.aws_lb_controller.arn
  }

  depends_on = [
    aws_eks_node_group.main,
    aws_iam_role_policy_attachment.aws_lb_controller
  ]
}

# ============================================
# Redis Deployment
# ============================================
resource "kubernetes_deployment" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "redis"
      }
    }

    template {
      metadata {
        labels = {
          app = "redis"
        }
      }

      spec {
        container {
          name  = "redis"
          image = "redis:7-alpine"

          port {
            container_port = 6379
          }

          resources {
            requests = {
              memory = "128Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "256Mi"
              cpu    = "200m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_namespace.uit_go]
}

resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "redis"
    }

    port {
      port        = 6379
      target_port = 6379
    }
  }
}

# ============================================
# RabbitMQ Deployment
# ============================================
resource "kubernetes_deployment" "rabbitmq" {
  metadata {
    name      = "rabbitmq"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "rabbitmq"
      }
    }

    template {
      metadata {
        labels = {
          app = "rabbitmq"
        }
      }

      spec {
        container {
          name  = "rabbitmq"
          image = "rabbitmq:3.13-management-alpine"

          port {
            container_port = 5672
            name           = "amqp"
          }

          port {
            container_port = 15672
            name           = "management"
          }

          env {
            name  = "RABBITMQ_DEFAULT_USER"
            value = "guest"
          }

          env {
            name  = "RABBITMQ_DEFAULT_PASS"
            value = "guest"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "200m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "400m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_namespace.uit_go]
}

resource "kubernetes_service" "rabbitmq" {
  metadata {
    name      = "rabbitmq"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "rabbitmq"
    }

    port {
      name        = "amqp"
      port        = 5672
      target_port = 5672
    }

    port {
      name        = "management"
      port        = 15672
      target_port = 15672
    }
  }
}

# ============================================
# User Service Database
# ============================================
resource "kubernetes_deployment" "user_db" {
  metadata {
    name      = "user-service-db"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "user-service-db"
      }
    }

    template {
      metadata {
        labels = {
          app = "user-service-db"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:15"

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = "user_service_db"
          }

          env {
            name  = "POSTGRES_USER"
            value = "user_service_user"
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = "user_service_pass"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "200m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_namespace.uit_go]
}

resource "kubernetes_service" "user_db" {
  metadata {
    name      = "user-service-db"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "user-service-db"
    }

    port {
      port        = 5432
      target_port = 5432
    }
  }
}

# ============================================
# Trip Service Database (VN)
# ============================================
resource "kubernetes_deployment" "trip_db_vn" {
  metadata {
    name      = "trip-service-db-vn"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "trip-service-db-vn"
      }
    }

    template {
      metadata {
        labels = {
          app = "trip-service-db-vn"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:15"

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = "trip_service_db"
          }

          env {
            name  = "POSTGRES_USER"
            value = "trip_service_user"
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = "trip_service_pass"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "200m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_namespace.uit_go]
}

resource "kubernetes_service" "trip_db_vn" {
  metadata {
    name      = "trip-service-db-vn"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "trip-service-db-vn"
    }

    port {
      port        = 5432
      target_port = 5432
    }
  }
}

# ============================================
# Trip Service Database (TH)
# ============================================
resource "kubernetes_deployment" "trip_db_th" {
  metadata {
    name      = "trip-service-db-th"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "trip-service-db-th"
      }
    }

    template {
      metadata {
        labels = {
          app = "trip-service-db-th"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:15"

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = "trip_service_db"
          }

          env {
            name  = "POSTGRES_USER"
            value = "trip_service_user"
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = "trip_service_pass"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "200m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_namespace.uit_go]
}

resource "kubernetes_service" "trip_db_th" {
  metadata {
    name      = "trip-service-db-th"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "trip-service-db-th"
    }

    port {
      port        = 5432
      target_port = 5432
    }
  }
}
