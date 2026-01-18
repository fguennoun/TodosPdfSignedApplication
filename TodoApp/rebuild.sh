#!/bin/bash

# Script de nettoyage et reconstruction - Phase 3
# Exécuter depuis le répertoire TodoApp

echo "🧹 Nettoyage du cache Angular..."
rm -rf .angular/cache
echo "✅ Cache Angular supprimé"

echo ""
echo "🧹 Suppression de node_modules..."
rm -rf node_modules
echo "✅ node_modules supprimé"

echo ""
echo "📦 Réinstallation des dépendances..."
npm install
echo "✅ Dépendances réinstallées"

echo ""
echo "🔨 Compilation du projet..."
ng build
echo "✅ Compilation réussie"

echo ""
echo "🚀 Démarrage du serveur de développement..."
ng serve

echo ""
echo "✅ Serveur démarré sur http://localhost:4200"
