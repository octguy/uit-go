@echo off
REM push-images-to-ecr.bat - Build and push all Docker images to ECR

setlocal enabledelayedexpansion

REM Configuration
set AWS_REGION=ap-southeast-1
set PROJECT_NAME=uit-go

REM Get AWS Account ID
for /f %%i in ('aws sts get-caller-identity --query Account --output text') do set AWS_ACCOUNT_ID=%%i

if "%AWS_ACCOUNT_ID%"=="" (
    echo Error: Could not get AWS Account ID. Make sure AWS CLI is configured.
    exit /b 1
)

set ECR_URL=%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com

echo ==========================================
echo UIT-GO Docker Image Push to ECR
echo ==========================================
echo AWS Account: %AWS_ACCOUNT_ID%
echo ECR URL: %ECR_URL%
echo ==========================================

REM Login to ECR
echo.
echo Logging in to ECR...
aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ECR_URL%
if errorlevel 1 (
    echo Error: ECR login failed
    exit /b 1
)

REM Define services
set SERVICES=api-gateway user-service trip-service driver-service driver-simulator

REM Build and push each service
for %%s in (%SERVICES%) do (
    echo.
    echo ==========================================
    echo Building %%s...
    echo ==========================================
    
    cd /d "%~dp0..\..\backend\%%s"
    
    REM Build image
    docker build -t %PROJECT_NAME%/%%s:latest .
    if errorlevel 1 (
        echo Error: Build failed for %%s
        exit /b 1
    )
    
    REM Tag for ECR
    docker tag %PROJECT_NAME%/%%s:latest %ECR_URL%/%PROJECT_NAME%/%%s:latest
    
    REM Push to ECR
    echo Pushing %%s to ECR...
    docker push %ECR_URL%/%PROJECT_NAME%/%%s:latest
    if errorlevel 1 (
        echo Error: Push failed for %%s
        exit /b 1
    )
    
    echo %%s pushed successfully!
)

cd /d "%~dp0"

echo.
echo ==========================================
echo All images pushed to ECR successfully!
echo ==========================================
echo.
echo Next steps:
echo 1. Run 'terraform apply' to update ECS services
echo 2. Or manually update services with:
echo    aws ecs update-service --cluster %PROJECT_NAME%-cluster --service [service-name] --force-new-deployment

endlocal
