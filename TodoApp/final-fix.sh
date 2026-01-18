#!/bin/bash

# Script de solution définitive - Phase 3
# Exécuter depuis le répertoire TodoApp

echo "🛑 Arrêt du serveur (si en cours)..."
echo "Appuyez sur Ctrl+C si le serveur est en cours d'exécution"
echo ""

echo "🧹 Nettoyage des caches..."

# Supprimer les caches Angular
echo "  - Suppression du cache Angular..."
rm -rf .angular/cache

# Supprimer le dossier dist
echo "  - Suppression du dossier dist..."
rm -rf dist

# Supprimer les caches Vite
echo "  - Suppression des caches Vite..."
rm -rf node_modules/.vite
rm -rf node_modules/.cache

echo "✅ Caches supprimés"
echo ""

echo "📦 Réinstallation des dépendances..."
npm install
echo "✅ Dépendances réinstallées"
echo ""

echo "🚀 Démarrage du serveur de développement..."
echo "Accédez à http://localhost:4200"
echo ""

ng serve --poll=2000

echo ""
echo "✅ Serveur démarré sur http://localhost:4200"
