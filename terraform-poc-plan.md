# Terraform Proof of Concept Plan - UIT-Go Project

## Overview
This plan outlines implementing Terraform for local infrastructure management as a stepping stone before AWS deployment. We'll focus on containerized infrastructure using Docker provider since you're already using Docker Compose.

## Phase 1: Local Infrastructure with Terraform + Docker

### 1.1 Project Structure
```
terraform/
├── main.tf                 # Main configuration
├── variables.tf           # Input variables
├── outputs.tf            # Output values
├── providers.tf          # Provider configurations
├── docker.tf             # Docker resources
├── networks.tf           # Network configurations
├── databases.tf          # Database containers
└── services.tf           # Application services
```

### 1.2 What We'll Terraform-ize Locally
- **Docker Networks**: Create isolated networks for microservices
- **PostgreSQL Databases**: User DB, Trip DB, Driver DB
- **Application Services**: Spring Boot services, Go gRPC services
- **API Gateway**: Kong or Nginx
- **Monitoring**: Prometheus, Grafana (optional)

### 1.3 Benefits of Local Terraform PoC
- Learn Terraform syntax and concepts
- Test infrastructure as code principles
- Practice with state management
- Prepare for AWS migration
- Version control your infrastructure

## Phase 2: Terraform Configuration Details

### 2.1 Provider Configuration (`providers.tf`)
```hcl
terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
  required_version = ">= 1.0"
}

provider "docker" {
  host = "npipe:////.//pipe//docker_engine"  # Windows
}
```

### 2.2 Variables (`variables.tf`)
```hcl
variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "uit-go"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "database_passwords" {
  description = "Database passwords"
  type        = map(string)
  sensitive   = true
  default = {
    user_db   = "user_password"
    trip_db   = "trip_password"
    driver_db = "driver_password"
  }
}
```

### 2.3 Networks (`networks.tf`)
```hcl
# Main application network
resource "docker_network" "app_network" {
  name = "${var.project_name}-network"
  driver = "bridge"
}

# Database network (isolated)
resource "docker_network" "db_network" {
  name = "${var.project_name}-db-network"
  driver = "bridge"
  internal = true
}
```

### 2.4 Databases (`databases.tf`)
```hcl
# User Service Database
resource "docker_container" "user_db" {
  name  = "${var.project_name}-user-db"
  image = "postgres:15"
  
  env = [
    "POSTGRES_DB=user_service_db",
    "POSTGRES_USER=user_service_user",
    "POSTGRES_PASSWORD=${var.database_passwords.user_db}"
  ]
  
  networks_advanced {
    name = docker_network.db_network.name
  }
  
  networks_advanced {
    name = docker_network.app_network.name
  }
  
  ports {
    internal = 5432
    external = 5433
  }
  
  volumes {
    volume_name    = docker_volume.user_db_data.name
    container_path = "/var/lib/postgresql/data"
  }
}

# Database Volumes
resource "docker_volume" "user_db_data" {
  name = "${var.project_name}-user-db-data"
}
```

### 2.5 Application Services (`services.tf`)
```hcl
# User Service (Spring Boot)
resource "docker_container" "user_service" {
  name  = "${var.project_name}-user-service"
  image = "${var.project_name}/user-service:latest"
  
  depends_on = [docker_container.user_db]
  
  env = [
    "SPRING_PROFILES_ACTIVE=${var.environment}",
    "DB_HOST=user-db",
    "DB_PASSWORD=${var.database_passwords.user_db}"
  ]
  
  networks_advanced {
    name = docker_network.app_network.name
  }
  
  ports {
    internal = 8081
    external = 8081
  }
  
  restart = "unless-stopped"
}
```

## Phase 3: Implementation Steps

### 3.1 Prerequisites
1. **Install Terraform**:
   ```bash
   # Download from terraform.io or use Chocolatey
   choco install terraform
   ```

2. **Verify Installation**:
   ```bash
   terraform version
   ```

### 3.2 Setup Steps
1. **Create terraform directory**:
   ```bash
   mkdir terraform
   cd terraform
   ```

2. **Initialize Terraform**:
   ```bash
   terraform init
   ```

3. **Plan Infrastructure**:
   ```bash
   terraform plan
   ```

4. **Apply Infrastructure**:
   ```bash
   terraform apply
   ```

5. **Destroy Infrastructure** (when needed):
   ```bash
   terraform destroy
   ```

### 3.3 Migration from Docker Compose
We'll gradually move from `docker-compose.yml` to Terraform:

**Week 1**: Databases only
**Week 2**: Add Spring Boot services  
**Week 3**: Add Go gRPC services
**Week 4**: Add API Gateway and monitoring

## Phase 4: Advantages for AWS Migration

### 4.1 Transferable Concepts
- Resource dependencies
- Variable management
- State management
- Module organization
- Environment separation

### 4.2 AWS Resources (Future Phase)
```hcl
# Future AWS resources
resource "aws_vpc" "main" { }
resource "aws_rds_instance" "user_db" { }
resource "aws_ecs_cluster" "app_cluster" { }
resource "aws_iam_role" "service_role" { }
```

## Phase 5: Learning Path

### 5.1 Week 1: Fundamentals
- [ ] Install Terraform
- [ ] Learn basic syntax (resources, variables, outputs)
- [ ] Create simple Docker container with Terraform
- [ ] Understand terraform plan/apply/destroy cycle

### 5.2 Week 2: Intermediate
- [ ] Work with variables and locals
- [ ] Understand state management
- [ ] Create database containers
- [ ] Learn about dependencies

### 5.3 Week 3: Advanced
- [ ] Create modules for reusability
- [ ] Work with sensitive variables
- [ ] Implement proper networking
- [ ] Add data sources

### 5.4 Week 4: Production Ready
- [ ] Environment separation (dev/staging/prod)
- [ ] Remote state management
- [ ] CI/CD integration
- [ ] Documentation and best practices

## Phase 6: Success Metrics

### 6.1 Technical Goals
- [ ] Replace docker-compose with terraform for local dev
- [ ] Achieve same functionality as current setup
- [ ] Implement proper state management
- [ ] Create reusable modules

### 6.2 Learning Goals
- [ ] Understand Terraform core concepts
- [ ] Comfortable with HCL syntax
- [ ] Know how to debug Terraform issues
- [ ] Ready for AWS provider transition

## Phase 7: Next Steps After PoC

1. **AWS Account Setup**: Create AWS account and configure credentials
2. **AWS Provider**: Switch from Docker to AWS provider
3. **VPC Design**: Design proper VPC with subnets, security groups
4. **RDS Implementation**: Move databases to AWS RDS
5. **ECS/EKS**: Deploy services to AWS container services
6. **IAM**: Implement proper IAM roles and policies

## Getting Started Command

```bash
# 1. Create terraform directory
mkdir terraform
cd terraform

# 2. Create basic main.tf
# (Copy the configurations from above)

# 3. Initialize
terraform init

# 4. Plan
terraform plan

# 5. Apply
terraform apply
```

## File Priority Order
1. `providers.tf` - Set up Docker provider
2. `variables.tf` - Define input variables  
3. `networks.tf` - Create Docker networks
4. `databases.tf` - PostgreSQL containers
5. `services.tf` - Application containers
6. `outputs.tf` - Export important values

This plan gives you a solid foundation to learn Terraform while working with familiar Docker concepts, preparing you for eventual AWS deployment.