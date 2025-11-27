# variables.tf - Define all configurable variables

variable "namespace" {
  description = "Kubernetes namespace for the application"
  type        = string
  default     = "default"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

# Database Configuration
variable "db_configs" {
  description = "Database configurations for each service"
  type = map(object({
    user     = string
    password = string
    name     = string
  }))
  default = {
    user = {
      user     = "user_service_user"
      password = "user_service_pass"
      name     = "user_service_db"
    }
    trip = {
      user     = "trip_service_user"
      password = "trip_service_pass"
      name     = "trip_service_db"
    }
    driver = {
      user     = "driver_service_user"
      password = "driver_service_pass"
      name     = "driver_service_db"
    }
  }
}

# Service Configuration
variable "services" {
  description = "Service configurations"
  type = map(object({
    image           = string
    port            = number
    image_pull_policy = string
    replicas        = number
  }))
  default = {
    user-service = {
      image           = "user-service:latest"
      port            = 8081
      image_pull_policy = "Never"
      replicas        = 1
    }
    trip-service = {
      image           = "trip-service:latest"
      port            = 8082
      image_pull_policy = "Never"
      replicas        = 1
    }
    driver-service = {
      image           = "driver-service:latest"
      port            = 8083
      image_pull_policy = "Never"
      replicas        = 1
    }
  }
}

# gRPC Service Configuration
variable "grpc_services" {
  description = "gRPC service configurations"
  type = map(object({
    image           = string
    port            = number
    image_pull_policy = string
    replicas        = number
  }))
  default = {
    user-grpc = {
      image           = "user-grpc:latest"
      port            = 50051
      image_pull_policy = "Never"
      replicas        = 1
    }
    trip-grpc = {
      image           = "trip-grpc:latest"
      port            = 50052
      image_pull_policy = "Never"
      replicas        = 1
    }
    driver-grpc = {
      image           = "driver-grpc:latest"
      port            = 50053
      image_pull_policy = "Never"
      replicas        = 1
    }
  }
}

variable "postgres_image" {
  description = "PostgreSQL Docker image"
  type        = string
  default     = "postgres:15-alpine"
}

# Resource limits (optional)
variable "resource_limits" {
  description = "Resource limits for containers"
  type = object({
    cpu_request    = string
    cpu_limit      = string
    memory_request = string
    memory_limit   = string
  })
  default = {
    cpu_request    = "100m"
    cpu_limit      = "500m"
    memory_request = "128Mi"
    memory_limit   = "512Mi"
  }
}