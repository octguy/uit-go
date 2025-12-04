# outputs.tf - Output values

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "api_gateway_url" {
  description = "URL to access the API Gateway"
  value       = "http://${aws_lb.main.dns_name}"
}

output "ecr_repositories" {
  description = "ECR repository URLs for pushing Docker images"
  value = {
    for name, repo in aws_ecr_repository.services : name => repo.repository_url
  }
}

output "user_db_endpoint" {
  description = "User service database endpoint"
  value       = aws_db_instance.user_db.endpoint
}

output "trip_db_vn_endpoint" {
  description = "Trip service (VN) database endpoint"
  value       = aws_db_instance.trip_db_vn.endpoint
}

output "trip_db_th_endpoint" {
  description = "Trip service (TH) database endpoint"
  value       = aws_db_instance.trip_db_th.endpoint
}

output "redis_endpoint" {
  description = "Redis cluster endpoint"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "rabbitmq_endpoint" {
  description = "RabbitMQ broker endpoint"
  value       = aws_mq_broker.rabbitmq.instances[0].endpoints[0]
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}
