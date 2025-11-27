@echo off
echo Applying all Kubernetes manifests...

REM ================================
REM Apply all YAML files at once
REM ================================
echo Deploying all services and databases...

kubectl apply -f infra\k8s\

echo All manifests applied!

REM ================================
REM Wait for deployments to be ready
REM ================================
echo Waiting for all deployments to be ready...

kubectl wait --for=condition=available --timeout=300s deployment --all

echo.
echo Checking final status...
kubectl get pods
echo.
kubectl get services

echo.
echo Deployment completed!
echo All services should be running now.

pause