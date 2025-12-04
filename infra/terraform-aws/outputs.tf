# outputs.tf - Output values for EKS deployment

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_ca_certificate" {
  description = "EKS cluster CA certificate (base64 encoded)"
  value       = aws_eks_cluster.main.certificate_authority[0].data
  sensitive   = true
}

output "ecr_repositories" {
  description = "ECR repository URLs for pushing Docker images"
  value = {
    for name, repo in aws_ecr_repository.services : name => repo.repository_url
  }
}

output "configure_kubectl" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}

output "api_gateway_ingress" {
  description = "Get the API Gateway URL with this command after deployment"
  value       = "kubectl get ingress -n uit-go api-gateway-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'"
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "namespace" {
  description = "Kubernetes namespace for the application"
  value       = "uit-go"
}
