@echo off
REM Go up one level first, then build

start cmd /k "cd /d %~dp0..\backend\user-service && call mvnw.cmd clean package -DskipTests"
start cmd /k "cd /d %~dp0..\backend\trip-service && call mvnw.cmd clean package -DskipTests"
start cmd /k "cd /d %~dp0..\backend\driver-service && call mvnw.cmd clean package -DskipTests"
start cmd /k "cd /d %~dp0..\backend\api-gateway && call mvnw.cmd clean package -DskipTests"

echo Build commands started in separate terminals!
