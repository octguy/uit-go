# databases.tf - PostgreSQL databases for each service

# User Service Database
resource "kubernetes_deployment" "user_service_db" {
  metadata {
    name      = "user-service-db"
    namespace = var.namespace
    labels = {
      app         = "user-service-db"
      environment = var.environment
    }
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
          app         = "user-service-db"
          environment = var.environment
        }
      }

      spec {
        container {
          name  = "user-service-db"
          image = var.postgres_image

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.db_configs.user.name
          }

          env {
            name  = "POSTGRES_USER"
            value = var.db_configs.user.user
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = var.db_configs.user.password
          }

          volume_mount {
            name       = "user-db-data"
            mount_path = "/var/lib/postgresql/data"
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

        volume {
          name = "user-db-data"
          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service" "user_service_db" {
  metadata {
    name      = "user-service-db"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.user_service_db.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = 5432
      target_port = 5432
    }

    type = "ClusterIP"
  }
}

# Trip Service Database
resource "kubernetes_deployment" "trip_service_db" {
  metadata {
    name      = "trip-service-db"
    namespace = var.namespace
    labels = {
      app         = "trip-service-db"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "trip-service-db"
      }
    }

    template {
      metadata {
        labels = {
          app         = "trip-service-db"
          environment = var.environment
        }
      }

      spec {
        container {
          name  = "trip-service-db"
          image = var.postgres_image

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.db_configs.trip.name
          }

          env {
            name  = "POSTGRES_USER"
            value = var.db_configs.trip.user
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = var.db_configs.trip.password
          }

          volume_mount {
            name       = "trip-db-data"
            mount_path = "/var/lib/postgresql/data"
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

        volume {
          name = "trip-db-data"
          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service" "trip_service_db" {
  metadata {
    name      = "trip-service-db"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.trip_service_db.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = 5432
      target_port = 5432
    }

    type = "ClusterIP"
  }
}

# Driver Service Database
resource "kubernetes_deployment" "driver_service_db" {
  metadata {
    name      = "driver-service-db"
    namespace = var.namespace
    labels = {
      app         = "driver-service-db"
      environment = var.environment
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "driver-service-db"
      }
    }

    template {
      metadata {
        labels = {
          app         = "driver-service-db"
          environment = var.environment
        }
      }

      spec {
        container {
          name  = "driver-service-db"
          image = var.postgres_image

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.db_configs.driver.name
          }

          env {
            name  = "POSTGRES_USER"
            value = var.db_configs.driver.user
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value = var.db_configs.driver.password
          }

          volume_mount {
            name       = "driver-db-data"
            mount_path = "/var/lib/postgresql/data"
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

        volume {
          name = "driver-db-data"
          empty_dir {}
        }
      }
    }
  }
}

resource "kubernetes_service" "driver_service_db" {
  metadata {
    name      = "driver-service-db"
    namespace = var.namespace
  }

  spec {
    selector = {
      app = kubernetes_deployment.driver_service_db.metadata.0.labels.app
    }

    port {
      protocol    = "TCP"
      port        = 5432
      target_port = 5432
    }

    type = "ClusterIP"
  }
}