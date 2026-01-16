# Script de démarrage pour l'environnement de développement Todo Application

Write-Host "=== Démarrage de l'environnement Todo Application ===" -ForegroundColor Green

# Vérifier si Docker est installé et démarré
Write-Host "Vérification de Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    docker info | Out-Null
    Write-Host "✓ Docker est disponible" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker n'est pas disponible. Veuillez installer et démarrer Docker Desktop." -ForegroundColor Red
    exit 1
}

# Démarrer les services externes avec Docker Compose
Write-Host "Démarrage des services externes (Kafka, Redis)..." -ForegroundColor Yellow
docker-compose up -d

# Attendre que les services soient prêts
Write-Host "Attente du démarrage des services..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Vérifier si les services sont prêts
Write-Host "Vérification des services..." -ForegroundColor Yellow

# Test Kafka
try {
    $kafkaTest = Test-NetConnection -ComputerName localhost -Port 9092 -WarningAction SilentlyContinue
    if ($kafkaTest.TcpTestSucceeded) {
        Write-Host "✓ Kafka est prêt (port 9092)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Kafka n'est pas encore prêt" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Impossible de vérifier Kafka" -ForegroundColor Yellow
}

# Test Redis
try {
    $redisTest = Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue
    if ($redisTest.TcpTestSucceeded) {
        Write-Host "✓ Redis est prêt (port 6379)" -ForegroundColor Green
    } else {
        Write-Host "⚠ Redis n'est pas encore prêt" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Impossible de vérifier Redis" -ForegroundColor Yellow
}

Write-Host "`n=== Services démarrés ===" -ForegroundColor Green
Write-Host "Kafka UI: http://localhost:8090" -ForegroundColor Cyan
Write-Host "Redis Commander: http://localhost:8091" -ForegroundColor Cyan
Write-Host "Application Backend: http://localhost:8080 (après démarrage Spring Boot)" -ForegroundColor Cyan
Write-Host "Application Frontend: http://localhost:4200 (après démarrage Angular)" -ForegroundColor Cyan

Write-Host "`n=== Prochaines étapes ===" -ForegroundColor Yellow
Write-Host "1. Démarrer l'application Spring Boot:" -ForegroundColor White
Write-Host "   cd TodoApplication && mvn spring-boot:run" -ForegroundColor Gray
Write-Host "2. Démarrer l'application Angular:" -ForegroundColor White
Write-Host "   cd TodoApp && npm install && ng serve" -ForegroundColor Gray

Write-Host "`n=== Pour arrêter les services ===" -ForegroundColor Yellow
Write-Host "docker-compose down" -ForegroundColor Gray
