# main.tf - Main Terraform configuration for UIT-GO microservices on Kubernetes

# This file serves as the entry point for the Terraform configuration.
# It includes all the necessary resources for the UIT-GO ride-sharing platform:
# - PostgreSQL databases for each service
# - REST API services (user, trip, driver)
# - Kubernetes services and networking

# The actual resource definitions are organized in separate files:
# - providers.tf: Terraform and Kubernetes provider configuration
# - variables.tf: All configurable variables and their defaults
# - databases.tf: PostgreSQL database deployments and services
# - services.tf: Main application services (REST APIs)
# - outputs.tf: Important information about deployed resources

# To deploy this infrastructure:
# 1. Make sure you have Terraform installed
# 2. Make sure you have kubectl configured for your Kubernetes cluster
# 3. Run: terraform init
# 4. Run: terraform plan
# 5. Run: terraform apply

# To customize the deployment, you can create a terraform.tfvars file
# or use -var flags to override the default values in variables.tf

locals {
  common_labels = {
    project     = "uit-go"
    environment = var.environment
    managed_by  = "terraform"
  }
}

# Data source to get information about the current Kubernetes cluster
data "kubernetes_namespace" "current" {
  metadata {
    name = var.namespace
  }
}