# k8s-services.tf - Application services (API Gateway, User, Trip, Driver)

# ============================================
# User Service
# ============================================
resource "kubernetes_deployment" "user_service" {
  metadata {
    name      = "user-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

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
          name  = "user-service"
          image = "${aws_ecr_repository.services["user-service"].repository_url}:latest"

          port {
            container_port = 8081
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "default"
          }

          env {
            name  = "DB_HOST"
            value = "user-service-db"
          }

          env {
            name  = "DB_PORT"
            value = "5432"
          }

          env {
            name  = "DB_NAME"
            value = "user_service_db"
          }

          env {
            name  = "DB_USER"
            value = "user_service_user"
          }

          env {
            name  = "DB_PASSWORD"
            value = "user_service_pass"
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

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8081
            }
            initial_delay_seconds = 60
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8081
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.user_db,
    aws_ecr_repository.services
  ]
}

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
  }
}

# ============================================
# Trip Service
# ============================================
resource "kubernetes_deployment" "trip_service" {
  metadata {
    name      = "trip-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "trip-service"
      }
    }

    template {
      metadata {
        labels = {
          app = "trip-service"
        }
      }

      spec {
        container {
          name  = "trip-service"
          image = "${aws_ecr_repository.services["trip-service"].repository_url}:latest"

          port {
            container_port = 8082
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "default"
          }

          env {
            name  = "DB_VN_HOST"
            value = "trip-service-db-vn"
          }

          env {
            name  = "DB_TH_HOST"
            value = "trip-service-db-th"
          }

          env {
            name  = "DB_PORT"
            value = "5432"
          }

          env {
            name  = "DB_NAME"
            value = "trip_service_db"
          }

          env {
            name  = "DB_USER"
            value = "trip_service_user"
          }

          env {
            name  = "DB_PASSWORD"
            value = "trip_service_pass"
          }

          env {
            name  = "RABBITMQ_HOST"
            value = "rabbitmq"
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

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8082
            }
            initial_delay_seconds = 60
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8082
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.trip_db_vn,
    kubernetes_deployment.trip_db_th,
    kubernetes_deployment.rabbitmq,
    aws_ecr_repository.services
  ]
}

resource "kubernetes_service" "trip_service" {
  metadata {
    name      = "trip-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "trip-service"
    }

    port {
      port        = 8082
      target_port = 8082
    }
  }
}

# ============================================
# Driver Service
# ============================================
resource "kubernetes_deployment" "driver_service" {
  metadata {
    name      = "driver-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "driver-service"
      }
    }

    template {
      metadata {
        labels = {
          app = "driver-service"
        }
      }

      spec {
        container {
          name  = "driver-service"
          image = "${aws_ecr_repository.services["driver-service"].repository_url}:latest"

          port {
            container_port = 8083
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "default"
          }

          env {
            name  = "REDIS_HOST"
            value = "redis"
          }

          env {
            name  = "REDIS_PORT"
            value = "6379"
          }

          env {
            name  = "RABBITMQ_HOST"
            value = "rabbitmq"
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

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8083
            }
            initial_delay_seconds = 60
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8083
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.redis,
    kubernetes_deployment.rabbitmq,
    aws_ecr_repository.services
  ]
}

resource "kubernetes_service" "driver_service" {
  metadata {
    name      = "driver-service"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "driver-service"
    }

    port {
      port        = 8083
      target_port = 8083
    }
  }
}

# ============================================
# Driver Simulator
# ============================================
resource "kubernetes_deployment" "driver_simulator" {
  metadata {
    name      = "driver-simulator"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "driver-simulator"
      }
    }

    template {
      metadata {
        labels = {
          app = "driver-simulator"
        }
      }

      spec {
        container {
          name  = "driver-simulator"
          image = "${aws_ecr_repository.services["driver-simulator"].repository_url}:latest"

          port {
            container_port = 8084
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "default"
          }

          env {
            name  = "DRIVER_SERVICE_URL"
            value = "http://driver-service:8083"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "250m"
            }
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.driver_service,
    aws_ecr_repository.services
  ]
}

resource "kubernetes_service" "driver_simulator" {
  metadata {
    name      = "driver-simulator"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "driver-simulator"
    }

    port {
      port        = 8084
      target_port = 8084
    }
  }
}

# ============================================
# API Gateway
# ============================================
resource "kubernetes_deployment" "api_gateway" {
  metadata {
    name      = "api-gateway"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "api-gateway"
      }
    }

    template {
      metadata {
        labels = {
          app = "api-gateway"
        }
      }

      spec {
        container {
          name  = "api-gateway"
          image = "${aws_ecr_repository.services["api-gateway"].repository_url}:latest"

          port {
            container_port = 8080
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "default"
          }

          env {
            name  = "USER_SERVICE_URL"
            value = "http://user-service:8081"
          }

          env {
            name  = "TRIP_SERVICE_URL"
            value = "http://trip-service:8082"
          }

          env {
            name  = "DRIVER_SERVICE_URL"
            value = "http://driver-service:8083"
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

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_deployment.user_service,
    kubernetes_deployment.trip_service,
    kubernetes_deployment.driver_service,
    aws_ecr_repository.services
  ]
}

resource "kubernetes_service" "api_gateway" {
  metadata {
    name      = "api-gateway"
    namespace = kubernetes_namespace.uit_go.metadata[0].name
  }

  spec {
    selector = {
      app = "api-gateway"
    }

    port {
      port        = 8080
      target_port = 8080
    }
  }
}

# ============================================
# Ingress (Load Balancer)
# ============================================
resource "kubernetes_ingress_v1" "api_gateway" {
  metadata {
    name      = "api-gateway-ingress"
    namespace = kubernetes_namespace.uit_go.metadata[0].name

    annotations = {
      "kubernetes.io/ingress.class"               = "alb"
      "alb.ingress.kubernetes.io/scheme"          = "internet-facing"
      "alb.ingress.kubernetes.io/target-type"     = "ip"
      "alb.ingress.kubernetes.io/healthcheck-path" = "/actuator/health"
    }
  }

  spec {
    rule {
      http {
        path {
          path      = "/"
          path_type = "Prefix"

          backend {
            service {
              name = kubernetes_service.api_gateway.metadata[0].name
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }

  depends_on = [helm_release.aws_lb_controller]
}
