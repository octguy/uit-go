# variables.tf - Input variables for UIT-GO AWS Infrastructure

variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "ap-southeast-1" # Singapore - close to Vietnam
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "uit-go"
}

# VPC Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

# Database Configuration
variable "db_username" {
  description = "Master username for RDS"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Master password for RDS"
  type        = string
  sensitive   = true
}

# Service Configuration
variable "services" {
  description = "Service configurations"
  type = map(object({
    port     = number
    cpu      = number
    memory   = number
    replicas = number
  }))
  default = {
    api-gateway = {
      port     = 8080
      cpu      = 256
      memory   = 512
      replicas = 1
    }
    user-service = {
      port     = 8081
      cpu      = 256
      memory   = 512
      replicas = 1
    }
    trip-service = {
      port     = 8082
      cpu      = 256
      memory   = 512
      replicas = 1
    }
    driver-service = {
      port     = 8083
      cpu      = 256
      memory   = 512
      replicas = 1
    }
    driver-simulator = {
      port     = 8084
      cpu      = 256
      memory   = 512
      replicas = 1
    }
  }
}
