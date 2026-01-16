# Todo Application - Phase 1 : Asynchrone et Messaging

## Vue d'ensemble de la Phase 1

Cette phase implémente les fonctionnalités asynchrones et messaging pour améliorer les performances et l'expérience utilisateur :

### ✅ Fonctionnalités implémentées

#### 1. **Traitement Asynchrone des PDFs volumineux**
- Génération de PDFs en arrière-plan avec Kafka
- Notifications temps réel via WebSocket
- Pool de threads dédié pour le traitement PDF
- Gestion des erreurs et retry automatique

#### 2. **Queue Kafka pour la synchronisation JSONPlaceholder**
- Synchronisation en lot avec queue Kafka
- Traitement asynchrone des données externes
- Notifications de progression en temps réel
- Gestion de la charge avec backpressure

#### 3. **WebSockets pour les notifications temps réel**
- Notifications instantanées pour les utilisateurs
- Mises à jour de progression des tâches
- Support multi-utilisateur avec routing personnalisé
- Gestion des connexions persistantes

#### 4. **Configuration et Infrastructure**
- Apache Kafka pour le messaging
- Redis pour le cache et les sessions
- Pools de threads configurables
- Docker Compose pour l'environnement de développement

## Architecture des Messages

### Topics Kafka
- `pdf-processing` : Messages de traitement PDF
- `todo-sync` : Messages de synchronisation
- `notifications` : Messages de notification

### WebSocket Endpoints
- `/ws` : Point d'entrée WebSocket principal
- `/queue/notifications` : Notifications personnelles
- `/queue/pdf-updates` : Mises à jour PDF
- `/queue/sync-updates` : Mises à jour de synchronisation
- `/topic/system` : Notifications système globales

## Nouveaux Endpoints API

### Endpoints Asynchrones
```
POST /api/todos/sync-async          - Synchronisation asynchrone (ADMIN)
POST /api/todos/generate-bulk-pdf   - Génération PDF en lot (USER/ADMIN)
GET  /api/todos/task-status/{taskId} - Statut d'une tâche async (USER/ADMIN)
POST /api/todos/test-notification   - Test notification WebSocket (ADMIN)
```

## Services Principaux

### 1. KafkaMessageService
Gestion centralisée des messages Kafka :
```java
// Envoi de message PDF
kafkaMessageService.sendPdfProcessingMessage(message);

// Envoi de message de synchronisation
kafkaMessageService.sendTodoSyncMessage(message);

// Envoi de notification
kafkaMessageService.sendNotificationMessage(notification);
```

### 2. WebSocketNotificationService
Notifications temps réel via WebSocket :
```java
// Notification à un utilisateur spécifique
webSocketNotificationService.sendNotificationToUser(userId, notification);

// Notification à un topic
webSocketNotificationService.sendNotificationToTopic("system", notification);
```

### 3. PdfService (amélioré)
Traitement asynchrone des PDFs :
```java
// Génération asynchrone de PDF
CompletableFuture<String> taskId = pdfService.processLargePdfAsync(userId, todoId, todos);
```

### 4. TodoSyncService (amélioré)
Synchronisation asynchrone avec queue :
```java
// Synchronisation en lot asynchrone
CompletableFuture<String> batchId = todoSyncService.syncTodosBatchAsync(userId, todos, userMap);
```

## Configuration

### Application Properties
```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=todo-app

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# WebSocket
websocket.allowed-origins=http://localhost:4200,http://localhost:8080

# Async Processing
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=10
```

### Pools de Threads
- **pdfProcessingExecutor** : 2-5 threads pour PDFs
- **todoSyncExecutor** : 1-3 threads pour sync
- **notificationExecutor** : 1-2 threads pour notifications

## Démarrage Rapide

### 1. Services externes
```bash
# Démarrer Kafka, Redis avec Docker Compose
docker-compose up -d

# Ou utiliser le script PowerShell
.\start-dev.ps1
```

### 2. Application Backend
```bash
cd TodoApplication
mvn spring-boot:run
```

### 3. Test des fonctionnalités
```bash
# Test de notification (nécessite auth ADMIN)
curl -X POST http://localhost:8080/api/todos/test-notification \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Génération PDF asynchrone (nécessite auth USER)
curl -X POST http://localhost:8080/api/todos/generate-bulk-pdf \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Synchronisation asynchrone (nécessite auth ADMIN)
curl -X POST http://localhost:8080/api/todos/sync-async \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Monitoring et Debugging

### Kafka UI
- Interface Web : http://localhost:8090
- Visualisation des topics, messages, consumers

### Redis Commander
- Interface Web : http://localhost:8091
- Inspection du cache et des données

### Logs Application
```bash
# Suivre les logs en temps réel
tail -f logs/todo-application.log

# Logs spécifiques au messaging
grep "Kafka\|WebSocket" logs/todo-application.log
```

## Tests et Validation

### 1. Test de Connectivity WebSocket
```javascript
// Depuis la console du navigateur
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});
```

### 2. Test des Messages Kafka
```bash
# Consumer console pour voir les messages
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic notifications --from-beginning
```

### 3. Test de Performance
- Génération de 100+ todos puis PDF en lot
- Synchronisation de tous les todos JSONPlaceholder
- Monitoring des temps de réponse et ressources

## Prochaines Étapes

### Phase 2 (à venir)
1. **Optimisations avancées**
   - Cache Redis pour les données fréquentes
   - Compression des messages Kafka
   - Load balancing des WebSockets

2. **Monitoring et Observabilité**
   - Métriques Prometheus/Grafana
   - Tracing distribué avec Sleuth
   - Health checks avancés

3. **Sécurité renforcée**
   - Chiffrement des messages Kafka
   - Authentication WebSocket JWT
   - Rate limiting par utilisateur

## Troubleshooting

### Problèmes courants

1. **Kafka non disponible**
   ```bash
   # Vérifier le statut
   docker-compose ps
   # Redémarrer si nécessaire
   docker-compose restart kafka
   ```

2. **WebSocket connexion failed**
   - Vérifier les CORS dans WebSocketConfig
   - Valider le JWT token
   - Contrôler les logs de sécurité

3. **Redis connexion timeout**
   ```bash
   # Test de connexion Redis
   docker exec -it redis redis-cli ping
   ```

### Support et Debugging
- Logs détaillés : `logging.level.be.cm.todoapplication=DEBUG`
- Métriques JVM : http://localhost:8080/actuator/metrics
- Health check : http://localhost:8080/actuator/health
