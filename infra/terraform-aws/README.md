# UIT-GO AWS Terraform Infrastructure

This Terraform configuration deploys the UIT-GO ride-hailing platform on AWS.

## Architecture

```
                    ┌─────────────────────┐
                    │    Internet         │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │  Application Load   │
                    │     Balancer        │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼─────┐ ┌───────▼───────┐ ┌─────▼─────────┐
    │  API Gateway  │ │  User Service │ │ Trip Service  │
    │   (ECS)       │ │    (ECS)      │ │   (ECS)       │
    └───────────────┘ └───────┬───────┘ └───────┬───────┘
                              │                 │
    ┌─────────────────┐       │                 │
    │ Driver Service  │◄──────┼─────────────────┤
    │    (ECS)        │       │                 │
    └────────┬────────┘       │                 │
             │                │                 │
    ┌────────▼────────┐ ┌─────▼─────┐   ┌──────▼──────┐
    │   ElastiCache   │ │    RDS    │   │  Amazon MQ  │
    │   (Redis)       │ │ PostgreSQL│   │  RabbitMQ   │
    └─────────────────┘ └───────────┘   └─────────────┘
```

## AWS Services Used

| Service | Purpose |
|---------|---------|
| ECS Fargate | Container orchestration (serverless) |
| ECR | Docker image registry |
| RDS PostgreSQL | User & Trip databases |
| ElastiCache Redis | Driver location caching (geospatial) |
| Amazon MQ | RabbitMQ message broker |
| ALB | Load balancing & routing |
| VPC | Network isolation |
| CloudWatch | Logging |

## Prerequisites

1. **AWS CLI** configured with credentials
2. **Terraform** >= 1.0 installed
3. **Docker** for building images

## Quick Start

### 1. Initialize Terraform

```bash
cd infra/terraform-aws
terraform init
```

### 2. Configure Variables

```bash
# Copy example file
cp terraform.tfvars.example terraform.tfvars

# Edit with your values (set db_password!)
notepad terraform.tfvars
```

### 3. Deploy Infrastructure

```bash
# Preview changes
terraform plan

# Apply changes
terraform apply
```

### 4. Build & Push Docker Images

After infrastructure is created, push your images to ECR:

```bash
# Login to ECR
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com

# Build and push each service
cd backend/api-gateway
docker build -t uit-go/api-gateway .
docker tag uit-go/api-gateway:latest <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/uit-go/api-gateway:latest
docker push <account-id>.dkr.ecr.ap-southeast-1.amazonaws.com/uit-go/api-gateway:latest

# Repeat for other services...
```

Or use the provided script:

```bash
.\push-images-to-ecr.bat
```

### 5. Access Your Application

After deployment, get the ALB DNS:

```bash
terraform output api_gateway_url
```

## Outputs

| Output | Description |
|--------|-------------|
| `api_gateway_url` | Public URL to access the API |
| `ecr_repositories` | ECR URLs for pushing images |
| `user_db_endpoint` | User database connection string |
| `redis_endpoint` | Redis cluster address |
| `rabbitmq_endpoint` | RabbitMQ broker endpoint |

## Cost Estimation (Dev Environment)

| Resource | Type | Monthly Cost (approx) |
|----------|------|----------------------|
| ECS Fargate | 5 tasks (256 CPU, 512 MB) | ~$15 |
| RDS PostgreSQL | 3x db.t3.micro | ~$45 |
| ElastiCache Redis | cache.t3.micro | ~$12 |
| Amazon MQ | mq.t3.micro | ~$20 |
| NAT Gateway | 1 instance | ~$32 |
| ALB | 1 instance | ~$16 |
| **Total** | | **~$140/month** |

> **Tip**: For cost savings, you can:
> - Use NAT Instance instead of NAT Gateway (~$4/month)
> - Use smaller RDS instances
> - Stop services when not in use

## Cleanup

To destroy all resources:

```bash
terraform destroy
```

## Troubleshooting

### Services not starting

Check CloudWatch logs:

```bash
aws logs tail /ecs/uit-go --follow
```

### Database connection issues

Verify security groups allow traffic from ECS to RDS.

### Images not found

Ensure images are pushed to ECR with the `latest` tag.

## Files Structure

```
terraform-aws/
├── main.tf              # Provider configuration
├── variables.tf         # Input variables
├── vpc.tf               # VPC, subnets, routing
├── security-groups.tf   # Security groups
├── ecr.tf               # Container registry
├── rds.tf               # PostgreSQL databases
├── elasticache.tf       # Redis cluster
├── mq.tf                # RabbitMQ broker
├── ecs.tf               # ECS cluster & task definitions
├── ecs-services.tf      # ECS services
├── alb.tf               # Load balancer
├── outputs.tf           # Output values
└── terraform.tfvars     # Your configuration (create this)
```
