# ecr.tf - Elastic Container Registry for Docker images

resource "aws_ecr_repository" "services" {
  for_each = var.services

  name                 = "${var.project_name}/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }

  tags = {
    Name = "${var.project_name}-${each.key}"
  }
}

# Lifecycle policy to keep only last 5 images
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = {
        type = "expire"
      }
    }]
  })
}

# Output ECR URLs for pushing images
output "ecr_repository_urls" {
  description = "ECR repository URLs for each service"
  value = {
    for name, repo in aws_ecr_repository.services : name => repo.repository_url
  }
}
