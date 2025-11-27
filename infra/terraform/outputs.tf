# outputs.tf - Define outputs for important information

output "service_endpoints" {
  description = "Endpoints for all services"
  value = {
    user_service = {
      name = kubernetes_service.user_service.metadata.0.name
      port = var.services.user-service.port
      url  = "http://${kubernetes_service.user_service.metadata.0.name}:${var.services.user-service.port}"
    }
    trip_service = {
      name = kubernetes_service.trip_service.metadata.0.name
      port = var.services.trip-service.port
      url  = "http://${kubernetes_service.trip_service.metadata.0.name}:${var.services.trip-service.port}"
    }
    driver_service = {
      name = kubernetes_service.driver_service.metadata.0.name
      port = var.services.driver-service.port
      url  = "http://${kubernetes_service.driver_service.metadata.0.name}:${var.services.driver-service.port}"
    }
  }
}

output "grpc_service_endpoints" {
  description = "Endpoints for all gRPC services"
  value = {
    user_grpc_service = {
      name = kubernetes_service.user_grpc_service.metadata.0.name
      port = var.grpc_services.user-grpc.port
      url  = "${kubernetes_service.user_grpc_service.metadata.0.name}:${var.grpc_services.user-grpc.port}"
    }
    trip_grpc_service = {
      name = kubernetes_service.trip_grpc_service.metadata.0.name
      port = var.grpc_services.trip-grpc.port
      url  = "${kubernetes_service.trip_grpc_service.metadata.0.name}:${var.grpc_services.trip-grpc.port}"
    }
    driver_grpc_service = {
      name = kubernetes_service.driver_grpc_service.metadata.0.name
      port = var.grpc_services.driver-grpc.port
      url  = "${kubernetes_service.driver_grpc_service.metadata.0.name}:${var.grpc_services.driver-grpc.port}"
    }
  }
}

output "database_endpoints" {
  description = "Database connection information"
  value = {
    user_db = {
      host     = kubernetes_service.user_service_db.metadata.0.name
      port     = 5432
      database = var.db_configs.user.name
      username = var.db_configs.user.user
    }
    trip_db = {
      host     = kubernetes_service.trip_service_db.metadata.0.name
      port     = 5432
      database = var.db_configs.trip.name
      username = var.db_configs.trip.user
    }
    driver_db = {
      host     = kubernetes_service.driver_service_db.metadata.0.name
      port     = 5432
      database = var.db_configs.driver.name
      username = var.db_configs.driver.user
    }
  }
  sensitive = false
}

output "deployment_status" {
  description = "Status of all deployments"
  value = {
    databases = {
      user_service_db   = kubernetes_deployment.user_service_db.metadata.0.name
      trip_service_db   = kubernetes_deployment.trip_service_db.metadata.0.name
      driver_service_db = kubernetes_deployment.driver_service_db.metadata.0.name
    }
    services = {
      user_service   = kubernetes_deployment.user_service.metadata.0.name
      trip_service   = kubernetes_deployment.trip_service.metadata.0.name
      driver_service = kubernetes_deployment.driver_service.metadata.0.name
    }
    grpc_services = {
      user_grpc_service   = kubernetes_deployment.user_grpc_service.metadata.0.name
      trip_grpc_service   = kubernetes_deployment.trip_grpc_service.metadata.0.name
      driver_grpc_service = kubernetes_deployment.driver_grpc_service.metadata.0.name
    }
  }
}