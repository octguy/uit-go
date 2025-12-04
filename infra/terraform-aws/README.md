# UIT-GO AWS EKS Terraform Infrastructure

This Terraform configuration deploys the UIT-GO ride-hailing platform on **AWS EKS (Kubernetes)**.

## Architecture

```
                    ┌─────────────────────┐
                    │     Internet        │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   AWS ALB Ingress   │
                    │   (Load Balancer)   │
                    └──────────┬──────────┘
                               │
┌──────────────────────────────┼──────────────────────────────┐
│                         EKS Cluster                         │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                   uit-go namespace                      │ │
│  │                                                          │ │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │ │
│  │   │ API Gateway │  │User Service │  │Trip Service │    │ │
│  │   │  (Pod)      │  │   (Pod)     │  │   (Pod)     │    │ │
│  │   └─────────────┘  └──────┬──────┘  └──────┬──────┘    │ │
│  │                           │                │            │ │
│  │   ┌─────────────┐  ┌──────▼──────┐  ┌──────▼──────┐    │ │
│  │   │Driver Service│ │ User DB     │  │ Trip DB VN  │    │ │
│  │   │   (Pod)      │ │ (PostgreSQL)│  │ (PostgreSQL)│    │ │
│  │   └──────┬───────┘ └─────────────┘  └─────────────┘    │ │
│  │          │                                              │ │
│  │   ┌──────▼──────┐  ┌─────────────┐  ┌─────────────┐    │ │
│  │   │   Redis     │  │  RabbitMQ   │  │ Trip DB TH  │    │ │
│  │   │   (Pod)     │  │   (Pod)     │  │ (PostgreSQL)│    │ │
│  │   └─────────────┘  └─────────────┘  └─────────────┘    │ │
│  │                                                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────┐                                        │
│  │ Node Group      │ (2x t3.medium instances)               │
│  └─────────────────┘                                        │
└──────────────────────────────────────────────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │        ECR          │
                    │ (Docker Registry)   │
                    └─────────────────────┘
```

## What Gets Deployed

| Component | Type | Description |
|-----------|------|-------------|
| EKS Cluster | Kubernetes 1.29 | Managed Kubernetes control plane |
| Node Group | 2x t3.medium | Worker nodes for running pods |
| ECR | Container Registry | Stores Docker images |
| VPC | Networking | Isolated network with public/private subnets |
| ALB | Load Balancer | Internet-facing load balancer via Ingress |
| Pods | Containers | Redis, RabbitMQ, PostgreSQL, all services |

## Prerequisites

1. **AWS CLI** configured with credentials
2. **Terraform** >= 1.0
3. **kubectl** for Kubernetes management
4. **Docker** for building images

## Quick Start (Demo in 1 Day)

### Step 1: Initialize & Deploy Infrastructure

```bash
cd infra/terraform-aws

# Initialize Terraform
terraform init

# Deploy (takes ~15-20 minutes for EKS)
terraform apply
```

### Step 2: Configure kubectl

```bash
# Get the command from terraform output
aws eks update-kubeconfig --region ap-southeast-1 --name uit-go-cluster
```

### Step 3: Build & Push Docker Images

```bash
# Run the push script
.\push-images-to-ecr.bat
```

### Step 4: Verify Deployment

```bash
# Check pods are running
kubectl get pods -n uit-go

# Get the API Gateway URL
kubectl get ingress -n uit-go

# Test the API
curl http://<INGRESS_URL>/actuator/health
```

## Terraform Commands

```bash
# Initialize
terraform init

# Preview changes
terraform plan

# Apply changes
terraform apply

# Destroy everything (after demo)
terraform destroy
```

## Cost Estimate (1 Day Demo)

| Resource | Hourly Cost | 24h Cost |
|----------|-------------|----------|
| EKS Control Plane | $0.10 | $2.40 |
| 2x t3.medium nodes | $0.0416 × 2 | $2.00 |
| NAT Gateway | $0.045 | $1.08 |
| ALB | $0.0225 | $0.54 |
| **Total** | | **~$6-8** |

## Files Structure

```
terraform-aws/
├── main.tf              # Provider configuration
├── variables.tf         # Input variables
├── vpc.tf               # VPC, subnets, routing
├── security-groups.tf   # Security groups
├── ecr.tf               # Container registry
├── eks.tf               # EKS cluster & node group
├── eks-addons.tf        # EKS add-ons & LB controller
├── k8s-manifests.tf     # K8s infrastructure (Redis, RabbitMQ, DBs)
├── k8s-services.tf      # K8s application services
├── outputs.tf           # Output values
└── push-images-to-ecr.bat # Script to push images
```

## Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl get pods -n uit-go

# Check pod logs
kubectl logs -n uit-go <pod-name>

# Describe pod for events
kubectl describe pod -n uit-go <pod-name>
```

### Images not found

Make sure you've pushed images to ECR:
```bash
.\push-images-to-ecr.bat
```

### ALB not created

Wait a few minutes after deployment. Check AWS Load Balancer Controller:
```bash
kubectl get pods -n kube-system | grep aws-load-balancer
```

## Cleanup (Important!)

After your demo, destroy all resources to avoid charges:

```bash
terraform destroy
```

This will delete everything including the EKS cluster, nodes, and all Kubernetes resources.
