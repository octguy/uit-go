# providers.tf - Define Terraform providers

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

# Configure Kubernetes provider
provider "kubernetes" {
  # Configuration will be read from ~/.kube/config or KUBECONFIG environment variable
  # For local development with Docker Desktop or minikube
  config_path = "~/.kube/config"
  
  # Alternatively, you can configure it for specific contexts
  # config_context = "docker-desktop"
}