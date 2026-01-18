# Script de solution définitive - Phase 3
# Exécuter depuis le répertoire TodoApp

Write-Host "🛑 Arrêt du serveur (si en cours)..." -ForegroundColor Yellow
Write-Host "Appuyez sur Ctrl+C si le serveur est en cours d'exécution" -ForegroundColor Yellow
Write-Host ""

Write-Host "🧹 Nettoyage des caches..." -ForegroundColor Green

# Supprimer les caches Angular
Write-Host "  - Suppression du cache Angular..." -ForegroundColor Cyan
Remove-Item -Path ".angular/cache" -Recurse -Force -ErrorAction SilentlyContinue

# Supprimer le dossier dist
Write-Host "  - Suppression du dossier dist..." -ForegroundColor Cyan
Remove-Item -Path "dist" -Recurse -Force -ErrorAction SilentlyContinue

# Supprimer les caches Vite
Write-Host "  - Suppression des caches Vite..." -ForegroundColor Cyan
Remove-Item -Path "node_modules\.vite" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "node_modules\.cache" -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "✅ Caches supprimés" -ForegroundColor Green
Write-Host ""

Write-Host "📦 Réinstallation des dépendances..." -ForegroundColor Green
npm install
Write-Host "✅ Dépendances réinstallées" -ForegroundColor Green
Write-Host ""

Write-Host "🚀 Démarrage du serveur de développement..." -ForegroundColor Green
Write-Host "Accédez à http://localhost:4200" -ForegroundColor Cyan
Write-Host ""

ng serve --poll=2000

Write-Host ""
Write-Host "✅ Serveur démarré sur http://localhost:4200" -ForegroundColor Green
