@echo off
echo Applying all services to Kubernetes...

REM ================================
REM Deploy Databases first
REM ================================
echo Deploying databases...

kubectl apply -f infra\k8s\user-service-db-deployment.yaml
kubectl apply -f infra\k8s\user-service-db-service.yaml

kubectl apply -f infra\k8s\trip-service-db-deployment.yaml
kubectl apply -f infra\k8s\trip-service-db-service.yaml

kubectl apply -f infra\k8s\driver-service-db-deployment.yaml
kubectl apply -f infra\k8s\driver-service-db-service.yaml

echo Waiting for databases to be ready...
timeout /t 10 /nobreak

REM ================================
REM Deploy Spring Boot Services
REM ================================
echo Deploying Spring Boot services...

kubectl apply -f infra\k8s\user-service-deployment.yaml
kubectl apply -f infra\k8s\user-service-service.yaml

kubectl apply -f infra\k8s\trip-service-deployment.yaml
kubectl apply -f infra\k8s\trip-service-service.yaml

kubectl apply -f infra\k8s\driver-service-deployment.yaml
kubectl apply -f infra\k8s\driver-service-service.yaml

echo Waiting for services to be ready...
timeout /t 15 /nobreak

REM ================================
REM Deploy gRPC Services
REM ================================
echo Deploying gRPC services...

kubectl apply -f infra\k8s\user-grpc-service-deployment.yaml
kubectl apply -f infra\k8s\user-grpc-service-service.yaml

kubectl apply -f infra\k8s\trip-grpc-service-deployment.yaml
kubectl apply -f infra\k8s\trip-grpc-service-service.yaml

kubectl apply -f infra\k8s\driver-grpc-service-deployment.yaml
kubectl apply -f infra\k8s\driver-grpc-service-service.yaml

echo All services deployed successfully!

REM ================================
REM Check deployment status
REM ================================
echo Checking deployment status...
kubectl get pods
echo.
kubectl get services

echo.
echo Deployment completed!
echo You can check the status with:
echo   kubectl get pods
echo   kubectl get services
echo   kubectl logs ^<pod-name^>

pause