# Script de démarrage pour l'environnement de développement TodoApp avec PostgreSQL
# Ce script lance tous les services nécessaires avec Docker Compose

Write-Host "🚀 Démarrage de l'environnement TodoApp avec PostgreSQL..." -ForegroundColor Green

# Vérifier si Docker est démarré
try {
    docker info | Out-Null
    Write-Host "✅ Docker est en cours d'exécution" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker n'est pas démarré. Veuillez démarrer Docker Desktop" -ForegroundColor Red
    exit 1
}

# Construire et démarrer les services
Write-Host "📦 Construction et démarrage des services..." -ForegroundColor Yellow
docker-compose up --build -d

# Attendre que les services soient prêts
Write-Host "⏳ Attente que les services soient prêts..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Afficher le statut des services
Write-Host "📊 Statut des services:" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "🎉 Environnement démarré avec succès!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Services disponibles:" -ForegroundColor Cyan
Write-Host "  • TodoApp API: http://localhost:8080" -ForegroundColor White
Write-Host "  • PostgreSQL: localhost:5432 (todoapp/todouser)" -ForegroundColor White
Write-Host "  • Kafka UI: http://localhost:8090" -ForegroundColor White
Write-Host "  • Redis Commander: http://localhost:8091" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Commandes utiles:" -ForegroundColor Cyan
Write-Host "  • Voir les logs: docker-compose logs -f [service]" -ForegroundColor White
Write-Host "  • Arrêter: docker-compose down" -ForegroundColor White
Write-Host "  • Redémarrer un service: docker-compose restart [service]" -ForegroundColor White
Write-Host ""
Write-Host "💡 Pour se connecter à PostgreSQL:" -ForegroundColor Cyan
Write-Host "  docker exec -it postgres psql -U todouser -d todoapp" -ForegroundColor White
