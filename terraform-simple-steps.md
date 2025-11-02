# Terraform Learning Path - Super Concise

## Step 1: Install Terraform (5 minutes)
```bash
choco install terraform
terraform version
```

## Step 2: Create First Terraform File (10 minutes)
```bash
mkdir terraform
cd terraform
```

Create `main.tf`:
```hcl
# Just create one Docker container
resource "docker_container" "hello" {
  name  = "hello-terraform"
  image = "nginx:latest"
  ports {
    internal = 80
    external = 9080  # Different from your 8080-8083 range
  }
}
```

## Step 3: Run It (5 minutes)
```bash
terraform init
terraform plan
terraform apply
# Visit http://localhost:9080
terraform destroy
```

## Step 4: Add Your Database (15 minutes)
Replace `main.tf`:
```hcl
resource "docker_container" "user_db" {
  name  = "user-db-terraform"
  image = "postgres:15"
  env = [
    "POSTGRES_DB=user_service_db",
    "POSTGRES_USER=user_service_user", 
    "POSTGRES_PASSWORD=password123"
  ]
  ports {
    internal = 5432
    external = 5436  # Different from your 5433-5435 range
  }
}
```

Run:
```bash
terraform apply
```

## Step 5: Add Your Spring Boot Service (20 minutes)
Add to `main.tf`:
```hcl
resource "docker_container" "user_service" {
  name  = "user-service-terraform"
  image = "uit-go/user-service:latest"
  depends_on = [docker_container.user_db]
  env = [
    "DB_HOST=user-db-terraform",
    "DB_PASSWORD=password123"
  ]
  ports {
    internal = 8081
    external = 9081  # Different from your 8081 port
  }
}
```

## Step 6: Learn Variables (10 minutes)
Create `variables.tf`:
```hcl
variable "db_password" {
  default = "password123"
}
```

Update `main.tf` to use `var.db_password`

## Step 7: Full UIT-Go (30 minutes)
Add all your services: trip-service, driver-service, grpc services

## Final Step: Kubernetes (Later)
After mastering Steps 1-7, then move to Kubernetes

## Total Time: ~2 hours
Each step builds on the previous. Don't skip steps!