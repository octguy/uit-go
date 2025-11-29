@echo off
REM ================================
REM Deploy Infrastructure
REM ================================
echo Deploying Infrastructure...

kubectl apply -f infra\k8s\rabbitmq\rabbitmq-deployment.yaml
kubectl apply -f infra\k8s\rabbitmq\rabbitmq-service.yaml

kubectl apply -f infra\k8s\redis\redis-deployment.yaml
kubectl apply -f infra\k8s\redis\redis-service.yaml

kubectl apply -f infra\k8s\api-gateway\api-gateway-deployment.yaml
kubectl apply -f infra\k8s\api-gateway\api-gateway-service.yaml

echo Waiting for infrastructure to be ready...
timeout /t 10 /nobreak

REM ================================
REM Deploy Databases first
REM ================================
echo Deploying databases...

kubectl apply -f infra\k8s\user-db\user-service-db-deployment.yaml
kubectl apply -f infra\k8s\user-db\user-service-db-service.yaml

kubectl apply -f infra\k8s\trip-db\trip-service-db-vn-deployment.yaml
kubectl apply -f infra\k8s\trip-db\trip-service-db-vn-service.yaml

kubectl apply -f infra\k8s\trip-db\trip-service-db-th-deployment.yaml
kubectl apply -f infra\k8s\trip-db\trip-service-db-th-service.yaml

kubectl apply -f infra\k8s\driver-service\driver-service-db-deployment.yaml
kubectl apply -f infra\k8s\driver-service\driver-service-db-service.yaml

echo Waiting for databases to be ready...
timeout /t 10 /nobreak

REM ================================
REM Deploy Spring Boot Services
REM ================================
echo Deploying Spring Boot services...

kubectl apply -f infra\k8s\user-service\user-service-deployment.yaml
kubectl apply -f infra\k8s\user-service\user-service-service.yaml

kubectl apply -f infra\k8s\trip-service\trip-service-deployment.yaml
kubectl apply -f infra\k8s\trip-service\trip-service-service.yaml

kubectl apply -f infra\k8s\driver-service\driver-service-deployment.yaml
kubectl apply -f infra\k8s\driver-service\driver-service-service.yaml

echo Waiting for services to be ready...
timeout /t 15 /nobreak

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