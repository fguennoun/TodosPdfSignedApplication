# Script de nettoyage et reconstruction - Phase 3
# Exécuter depuis le répertoire TodoApp

Write-Host "🧹 Nettoyage du cache Angular..." -ForegroundColor Green
Remove-Item -Path ".angular/cache" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "✅ Cache Angular supprimé" -ForegroundColor Green

Write-Host "`n🧹 Suppression de node_modules..." -ForegroundColor Green
Remove-Item -Path "node_modules" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "✅ node_modules supprimé" -ForegroundColor Green

Write-Host "`n📦 Réinstallation des dépendances..." -ForegroundColor Green
npm install
Write-Host "✅ Dépendances réinstallées" -ForegroundColor Green

Write-Host "`n🔨 Compilation du projet..." -ForegroundColor Green
ng build
Write-Host "✅ Compilation réussie" -ForegroundColor Green

Write-Host "`n🚀 Démarrage du serveur de développement..." -ForegroundColor Green
ng serve

Write-Host "`n✅ Serveur démarré sur http://localhost:4200" -ForegroundColor Green
