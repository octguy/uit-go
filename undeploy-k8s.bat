@echo off
echo Removing all services from Kubernetes...

REM ================================
REM Remove gRPC Services first
REM ================================
echo Removing gRPC services...

kubectl delete -f infra\k8s\user-grpc-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\user-grpc-service-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\trip-grpc-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\trip-grpc-service-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\driver-grpc-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\driver-grpc-service-deployment.yaml --ignore-not-found=true

REM ================================
REM Remove Spring Boot Services
REM ================================
echo Removing Spring Boot services...

kubectl delete -f infra\k8s\user-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\user-service-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\trip-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\trip-service-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\driver-service-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\driver-service-deployment.yaml --ignore-not-found=true

REM ================================
REM Remove Databases last
REM ================================
echo Removing databases...

kubectl delete -f infra\k8s\user-service-db-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\user-service-db-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\trip-service-db-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\trip-service-db-deployment.yaml --ignore-not-found=true

kubectl delete -f infra\k8s\driver-service-db-service.yaml --ignore-not-found=true
kubectl delete -f infra\k8s\driver-service-db-deployment.yaml --ignore-not-found=true

echo All services removed successfully!

REM ================================
REM Check removal status
REM ================================
echo Checking removal status...
kubectl get pods
echo.
kubectl get services

echo.
echo Cleanup completed!

pause