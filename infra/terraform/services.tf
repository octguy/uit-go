# services.tf - Main application services (REST APIs)

# User Service
resource "kubernetes_deployment" "user_service" {
  metadata {
    name      = "user-service"
    namespace = var.namespace
    labels = {
      app         = "user-service"
      environment = var.environment
    }
  }

  spec {
    replicas = var.services.user-service.replicas

    selector {
      match_labels = {
        app = "user-service"
      }
    }

    template {
      metadata {
        labels = {
          app         = "user-service"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "user-service"
          image             = var.services.user-service.image
          image_pull_policy = var.services.user-service.image_pull_policy

          port {
            container_port = var.services.user-service.port
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.environment
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
            value = var.db_configs.user.name
          }

          env {
            name  = "DB_USER"
            value = var.db_configs.user.user
          }

          env {
            name  = "DB_PASSWORD"
            value = var.db_configs.user.password
          }

          resources {
            requests = {
              cpu    = var.resource_limits.cpu_request
              memory = var.resource_limits.memory_request
            }
            limits = {
              cpu    = var.resource_limits.cpu_limit
              memory = var.resource_limits.memory_limit
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.user_service_db]
}

resource "kubernetes_service" "user_service" {
  metadata {
    name      = "user-service"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.user_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.services.user-service.port
      target_port = var.services.user-service.port
    }

    type = "ClusterIP"
  }
}

# Trip Service
resource "kubernetes_deployment" "trip_service" {
  metadata {
    name      = "trip-service"
    namespace = var.namespace
    labels = {
      app         = "trip-service"
      environment = var.environment
    }
  }

  spec {
    replicas = var.services.trip-service.replicas

    selector {
      match_labels = {
        app = "trip-service"
      }
    }

    template {
      metadata {
        labels = {
          app         = "trip-service"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "trip-service"
          image             = var.services.trip-service.image
          image_pull_policy = var.services.trip-service.image_pull_policy

          port {
            container_port = var.services.trip-service.port
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.environment
          }

          env {
            name  = "DB_HOST"
            value = "trip-service-db"
          }

          env {
            name  = "DB_USER"
            value = var.db_configs.trip.user
          }

          env {
            name  = "DB_PASSWORD"
            value = var.db_configs.trip.password
          }

          env {
            name  = "DB_NAME"
            value = var.db_configs.trip.name
          }

          env {
            name  = "DB_PORT"
            value = "5432"
          }

          env {
            name  = "USER_GRPC_SERVICE_URL"
            value = "user-grpc:50051"
          }

          resources {
            requests = {
              cpu    = var.resource_limits.cpu_request
              memory = var.resource_limits.memory_request
            }
            limits = {
              cpu    = var.resource_limits.cpu_limit
              memory = var.resource_limits.memory_limit
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.trip_service_db]
}

resource "kubernetes_service" "trip_service" {
  metadata {
    name      = "trip-service"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.trip_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.services.trip-service.port
      target_port = var.services.trip-service.port
    }

    type = "ClusterIP"
  }
}

# Driver Service
resource "kubernetes_deployment" "driver_service" {
  metadata {
    name      = "driver-service"
    namespace = var.namespace
    labels = {
      app         = "driver-service"
      environment = var.environment
    }
  }

  spec {
    replicas = var.services.driver-service.replicas

    selector {
      match_labels = {
        app = "driver-service"
      }
    }

    template {
      metadata {
        labels = {
          app         = "driver-service"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "driver-service"
          image             = var.services.driver-service.image
          image_pull_policy = var.services.driver-service.image_pull_policy

          port {
            container_port = var.services.driver-service.port
          }

          env {
            name  = "SPRING_PROFILES_ACTIVE"
            value = var.environment
          }

          env {
            name  = "DB_HOST"
            value = "driver-service-db"
          }

          env {
            name  = "DB_PORT"
            value = "5432"
          }

          env {
            name  = "DB_NAME"
            value = var.db_configs.driver.name
          }

          env {
            name  = "DB_USER"
            value = var.db_configs.driver.user
          }

          env {
            name  = "DB_PASSWORD"
            value = var.db_configs.driver.password
          }

          resources {
            requests = {
              cpu    = var.resource_limits.cpu_request
              memory = var.resource_limits.memory_request
            }
            limits = {
              cpu    = var.resource_limits.cpu_limit
              memory = var.resource_limits.memory_limit
            }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.driver_service_db]
}

resource "kubernetes_service" "driver_service" {
  metadata {
    name      = "driver-service"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.driver_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.services.driver-service.port
      target_port = var.services.driver-service.port
    }

    type = "ClusterIP"
  }
}