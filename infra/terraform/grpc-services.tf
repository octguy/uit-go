# grpc-services.tf - gRPC services for inter-service communication

# User gRPC Service
resource "kubernetes_deployment" "user_grpc_service" {
  metadata {
    name      = "user-grpc"
    namespace = var.namespace
    labels = {
      app         = "user-grpc"
      environment = var.environment
    }
  }

  spec {
    replicas = var.grpc_services.user-grpc.replicas

    selector {
      match_labels = {
        app = "user-grpc"
      }
    }

    template {
      metadata {
        labels = {
          app         = "user-grpc"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "user-grpc"
          image             = var.grpc_services.user-grpc.image
          image_pull_policy = var.grpc_services.user-grpc.image_pull_policy

          port {
            container_port = var.grpc_services.user-grpc.port
          }

          env {
            name  = "SPRING_USER_SERVICE_URL"
            value = "http://user-service:8081"
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

  depends_on = [kubernetes_deployment.user_service]
}

resource "kubernetes_service" "user_grpc_service" {
  metadata {
    name      = "user-grpc"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.user_grpc_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.grpc_services.user-grpc.port
      target_port = var.grpc_services.user-grpc.port
    }

    type = "ClusterIP"
  }
}

# Trip gRPC Service
resource "kubernetes_deployment" "trip_grpc_service" {
  metadata {
    name      = "trip-grpc"
    namespace = var.namespace
    labels = {
      app         = "trip-grpc"
      environment = var.environment
    }
  }

  spec {
    replicas = var.grpc_services.trip-grpc.replicas

    selector {
      match_labels = {
        app = "trip-grpc"
      }
    }

    template {
      metadata {
        labels = {
          app         = "trip-grpc"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "trip-grpc"
          image             = var.grpc_services.trip-grpc.image
          image_pull_policy = var.grpc_services.trip-grpc.image_pull_policy

          port {
            container_port = var.grpc_services.trip-grpc.port
          }

          # Add environment variables as needed for trip gRPC service
          env {
            name  = "SPRING_TRIP_SERVICE_URL"
            value = "http://trip-service:8082"
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

  depends_on = [kubernetes_deployment.trip_service]
}

resource "kubernetes_service" "trip_grpc_service" {
  metadata {
    name      = "trip-grpc"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.trip_grpc_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.grpc_services.trip-grpc.port
      target_port = var.grpc_services.trip-grpc.port
    }

    type = "ClusterIP"
  }
}

# Driver gRPC Service
resource "kubernetes_deployment" "driver_grpc_service" {
  metadata {
    name      = "driver-grpc"
    namespace = var.namespace
    labels = {
      app         = "driver-grpc"
      environment = var.environment
    }
  }

  spec {
    replicas = var.grpc_services.driver-grpc.replicas

    selector {
      match_labels = {
        app = "driver-grpc"
      }
    }

    template {
      metadata {
        labels = {
          app         = "driver-grpc"
          environment = var.environment
        }
      }

      spec {
        container {
          name              = "driver-grpc"
          image             = var.grpc_services.driver-grpc.image
          image_pull_policy = var.grpc_services.driver-grpc.image_pull_policy

          port {
            container_port = var.grpc_services.driver-grpc.port
          }

          # Add environment variables as needed for driver gRPC service
          env {
            name  = "SPRING_DRIVER_SERVICE_URL"
            value = "http://driver-service:8083"
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

  depends_on = [kubernetes_deployment.driver_service]
}

resource "kubernetes_service" "driver_grpc_service" {
  metadata {
    name      = "driver-grpc"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.driver_grpc_service.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = var.grpc_services.driver-grpc.port
      target_port = var.grpc_services.driver-grpc.port
    }

    type = "ClusterIP"
  }
}