@echo off
title AirGuard Dev Server

echo.
echo  ╔═══════════════════════════════════════╗
echo  ║         AirGuard - Dev Mode           ║
echo  ║  Personalized Air Exposure Engine     ║
echo  ╚═══════════════════════════════════════╝
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Install Java 21+ from https://adoptium.net
    pause & exit /b 1
)

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found. Install from https://maven.apache.org
    pause & exit /b 1
)

echo [INFO] Building backend...
cd backend
call mvn -B package -DskipTests -q
if errorlevel 1 ( echo [ERROR] Build failed. & pause & exit /b 1 )
echo [OK] Backend built.

echo [INFO] Starting backend on port 8080...
start "AirGuard Backend" java -jar target\airguard-backend-*.jar

echo.
echo Waiting for backend to start (15s)...
timeout /t 15 /nobreak >nul

cd ..

echo.
echo ════════════════════════════════════════
echo   AirGuard is ready!
echo ════════════════════════════════════════
echo.
echo   Backend : http://localhost:8080/api
echo   Swagger : http://localhost:8080/api/swagger-ui.html
echo.
echo   Open frontend\index.html in your browser
echo   or install VS Code Live Server extension
echo.
pause
