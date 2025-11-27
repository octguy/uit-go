# UIT-GO Terraform Infrastructure

This directory contains the Terraform configuration for deploying the UIT-GO ride-sharing platform on Kubernetes.

## Architecture

The infrastructure consists of:

### Services
- **User Service**: REST API for user management (port 8081)
- **Trip Service**: REST API for trip management (port 8082)
- **Driver Service**: REST API for driver management (port 8083)

### gRPC Services
- **gRPC User Service**: Inter-service communication for user validation (port 50051)
- **gRPC Trip Service**: Inter-service communication for trip operations (port 50053)
- **gRPC Driver Service**: Inter-service communication for driver operations (port 50052)

### Databases
- **User Database**: PostgreSQL instance for user service (port 5432)
- **Trip Database**: PostgreSQL instance for trip service (port 5432)
- **Driver Database**: PostgreSQL instance for driver service (port 5432)

## File Structure

```
terraform/
├── main.tf                    # Main configuration and entry point
├── providers.tf               # Terraform and Kubernetes providers
├── variables.tf               # Variable definitions with defaults
├── databases.tf               # PostgreSQL database deployments
├── services.tf                # REST API service deployments
├── grpc-services.tf          # gRPC service deployments
├── outputs.tf                # Output values for deployed resources
├── terraform.tfvars.example  # Example variables file
└── README.md                 # This file
```

## Prerequisites

1. **Terraform**: Install Terraform (version 1.0+)
   ```bash
   # Download from https://terraform.io/downloads.html
   ```

2. **Kubernetes**: Have access to a Kubernetes cluster
   ```bash
   # Make sure kubectl is configured
   kubectl cluster-info
   ```

3. **Docker Images**: Build the required Docker images
   ```bash
   # Build all service images (from project root)
   docker build -t uit-go/user-service:latest ./user-service
   docker build -t uit-go/trip-service:latest ./trip-service
   docker build -t uit-go/driver-service:latest ./driver-service
   docker build -t uit-go/grpc-user-service:latest ./grpc/user
   docker build -t uit-go/grpc-trip-service:latest ./grpc/trip
   docker build -t uit-go/grpc-driver-service:latest ./grpc/driver
   ```

## Deployment

### 1. Initialize Terraform
```bash
cd infra/terraform
terraform init
```

### 2. Configure Variables (Optional)
Copy the example variables file and customize:
```bash
copy terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your preferred values
```

### 3. Plan the Deployment
```bash
terraform plan
```

### 4. Apply the Configuration
```bash
terraform apply
```

### 5. Verify Deployment
```bash
# Check if all pods are running
kubectl get pods

# Check services
kubectl get services

# Check if services are accessible
kubectl port-forward service/user-service 8081:8081
# Test: curl http://localhost:8081/users
```

## Configuration

### Environment Variables

Each service can be configured through variables in `terraform.tfvars`:

- `environment`: Environment name (dev, staging, prod)
- `namespace`: Kubernetes namespace
- Database passwords for each service
- Resource limits (CPU, memory)
- Number of replicas for scaling
- Docker image tags

### Resource Limits

Default resource limits are conservative for development:
- **Services**: 500m CPU, 512Mi memory
- **gRPC Services**: 500m CPU, 256Mi memory
- **Databases**: 500m CPU, 512Mi memory

Adjust these in `terraform.tfvars` based on your cluster capacity and workload requirements.

### Scaling

You can scale services by adjusting replica counts:
```hcl
user_service_replicas = 3
trip_service_replicas = 2
```

## Service URLs

After deployment, services will be available at:

- User Service: `http://user-service:8081`
- Trip Service: `http://trip-service:8082`
- Driver Service: `http://driver-service:8083`
- gRPC User Service: `user-grpc:50051`
- gRPC Trip Service: `trip-grpc:50053`
- gRPC Driver Service: `driver-grpc:50052`

## Accessing Services

### From Outside the Cluster
```bash
# Port forward to access services locally
kubectl port-forward service/user-service 8081:8081
kubectl port-forward service/trip-service 8082:8082
kubectl port-forward service/driver-service 8083:8083
```

### From Inside the Cluster
Services can communicate using their service names and ports as configured.

## Database Access

Each service has its own PostgreSQL database:

```bash
# Access user database
kubectl port-forward service/user-service-db 5433:5432
# Connect: postgresql://user_service_user:password123@localhost:5433/user_service_db

# Access trip database
kubectl port-forward service/trip-service-db 5434:5432
# Connect: postgresql://trip_service_user:password123@localhost:5434/trip_service_db

# Access driver database
kubectl port-forward service/driver-service-db 5435:5432
# Connect: postgresql://driver_service_user:password123@localhost:5435/driver_service_db
```

## Troubleshooting

### Check Pod Status
```bash
kubectl get pods
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Service Discovery Issues
```bash
kubectl get services
kubectl describe service <service-name>
```

### Database Connection Issues
```bash
# Check database pod logs
kubectl logs <database-pod-name>

# Test database connectivity
kubectl exec -it <service-pod-name> -- nc -zv <database-service-name> 5432
```

### gRPC Communication Issues
```bash
# Check gRPC service logs
kubectl logs <grpc-service-pod-name>

# Test gRPC connectivity
kubectl exec -it <service-pod-name> -- nc -zv <grpc-service-name> <grpc-port>
```

## Cleanup

To destroy all resources:
```bash
terraform destroy
```

## Development Workflow

1. Make code changes
2. Rebuild Docker images
3. Update image tags in `terraform.tfvars` (if using versioned tags)
4. Run `terraform apply` to deploy changes
5. Test the updated services

For rapid development, you can use `imagePullPolicy: Never` in the Kubernetes deployments and rebuild images locally without pushing to a registry.

## Production Considerations

For production deployments:

1. **Security**: Change default passwords, use Kubernetes secrets
2. **Monitoring**: Add health checks and monitoring
3. **Persistence**: Use persistent volumes for databases
4. **Networking**: Configure ingress controllers for external access
5. **Scaling**: Adjust resource limits and replica counts
6. **Backup**: Implement database backup strategies

## Troubleshooting Common Issues

### Image Pull Issues
If you're using local images, make sure `imagePullPolicy` is set to `Never` and images are built on the correct nodes.

### Service Communication
Ensure services are using the correct service names and ports for internal communication.

### Database Initialization
Check database logs if services can't connect. Databases may need time to initialize on first startup.