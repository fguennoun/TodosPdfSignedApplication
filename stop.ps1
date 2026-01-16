# Script d'arrêt pour l'environnement TodoApp
Write-Host "🛑 Arrêt de l'environnement TodoApp..." -ForegroundColor Yellow

# Arrêter tous les services
docker-compose down

Write-Host "✅ Environnement arrêté" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Pour supprimer aussi les volumes (données persistantes):" -ForegroundColor Cyan
Write-Host "  docker-compose down -v" -ForegroundColor White
