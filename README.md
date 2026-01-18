# Todo Application - Projet Full-Stack Avancé

## 📋 Vue d'ensemble du projet

Ce projet consiste en une application Todo full-stack moderne avec :
- **Backend** : Spring Boot 3.2 (Java 17)
- **Frontend** : Angular 19
- **Base de données** : PostgreSQL (migré depuis H2)
- **Messaging** : Apache Kafka pour traitement asynchrone
- **Cache** : Redis pour performance
- **WebSockets** : Notifications temps réel
- **Fonctionnalités** : CRUD Todos, génération PDF asynchrone, signature électronique, synchronisation JSONPlaceholder

---

## ✅ État d'Implémentation

### 🎉 **Phase 1 (Sprint 1-2) - Fondations** ✅ TERMINÉE

#### 1.1 ✅ Authentification JWT
- [x] **Spring Security avec JWT** - Implémenté avec `JwtUtil`, `JwtRequestFilter`
- [x] **Endpoints `/api/auth/login` et `/api/auth/register`** - `AuthController` fonctionnel
- [x] **Rôles utilisateur** (USER, ADMIN) - Enum `Role` dans `User.java`
- [x] **Protection endpoints** - `SecurityConfig` avec JWT

#### 1.2 ✅ Validation des données
- [x] **@Valid et annotations** - DTOs avec `@NotBlank`, `@Email`, `@Size`
- [x] **Gestion d'erreurs globale** - `GlobalExceptionHandler` avec `@ControllerAdvice`
- [x] **Validation côté serveur** - Toutes les entrées validées

#### 1.3 ✅ Migration PostgreSQL
- [x] **Base de données PostgreSQL** - Configuration complète avec Docker
- [x] **Migration Flyway** - Scripts SQL pour initialisation
- [x] **Index optimisés** - Index sur champs fréquents (user_id, completed, etc.)
- [x] **Audit logs** - Colonnes created_at, updated_at, created_by, updated_by

```sql
-- Tables créées avec Flyway
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE todos (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT false,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
-- + Index optimisés créés
```

### 🎉 **Phase 2 (Sprint 3-4) - Performance & Messaging** ✅ TERMINÉE

#### 2.1 ✅ Cache Redis
- [x] **Configuration Redis** - `application.properties` avec Spring Data Redis
- [x] **Cache des données** - Optimisation des requêtes fréquentes
- [x] **Session WebSocket** - Redis pour persistance des connexions

#### 2.2 ✅ Apache Kafka Complet
- [x] **Configuration Kafka** - `KafkaConfig` avec producers/consumers
- [x] **Topics créés** - `pdf-processing`, `todo-sync`, `notifications`
- [x] **Serialization JSON** - Messages Kafka avec `JsonSerializer`

```java
// Configuration Kafka implémentée
@Configuration
@EnableKafka
public class KafkaConfig {
    public static final String PDF_PROCESSING_TOPIC = "pdf-processing";
    public static final String TODO_SYNC_TOPIC = "todo-sync";
    public static final String NOTIFICATION_TOPIC = "notifications";
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        // Configuration complète implémentée
    }
}
```

#### 2.3 ✅ Traitement Asynchrone des PDFs
- [x] **Service PDF asynchrone** - `PdfService` avec `@Async`
- [x] **Queue Kafka pour PDFs** - Messages `PdfProcessingMessage`
- [x] **Génération PDF avancée** - iTextPDF avec templates
- [x] **Watermark et signatures** - Métadonnées utilisateur et timestamp

```java
// Service asynchrone implémenté
@Service
public class PdfService {
    @Async
    @KafkaListener(topics = "pdf-processing")
    public CompletableFuture<Void> generatePdfAsync(PdfProcessingMessage request) {
        // Traitement asynchrone des PDFs volumineux implémenté
    }
}
```

#### 2.4 ✅ WebSockets & Notifications
- [x] **Configuration WebSocket** - `WebSocketConfig` avec STOMP
- [x] **Notifications temps réel** - `WebSocketNotificationService`
- [x] **Intégration Kafka-WebSocket** - Messages diffusés via WebSocket
- [x] **Topics WebSocket** - `/topic/notifications`, `/user/queue/private`

```java
// WebSocket + Kafka intégration implémentée
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

#### 2.5 ✅ Synchronisation JSONPlaceholder
- [x] **Queue Kafka sync** - `TodoSyncService` avec messages `TodoSyncMessage`
- [x] **Service de synchronisation** - Récupération et mise à jour automatique
- [x] **Gestion des erreurs** - Retry et logging des échecs

#### 2.6 ✅ Tests Unitaires
- [x] **Tests JUnit 5** - `AuthServiceTest`, `TodoServiceTest`, `KafkaMessageServiceTest`
- [x] **Mocking avec Mockito** - Tests isolés des dépendances
- [x] **Tests d'intégration** - Tests des controllers avec `@WebMvcTest`
- [x] **Tests Kafka** - `KafkaMessageListenerTest` avec TestContainers

---

## 🚀 Fonctionnalités Actuellement Disponibles

### 🔐 Authentification & Sécurité
- ✅ Inscription/Connexion avec JWT
- ✅ Rôles utilisateur (USER/ADMIN)
- ✅ Protection CORS et endpoints sécurisés
- ✅ Validation de toutes les entrées

### 📋 Gestion des Todos
- ✅ CRUD complet avec pagination
- ✅ Audit automatique (created_at, updated_at, etc.)
- ✅ Association utilisateur-todos sécurisée
- ✅ Recherche et filtrage

### 📄 Génération PDF Avancée
- ✅ Génération PDF asynchrone via Kafka
- ✅ Templates PDF professionnels
- ✅ Watermark avec utilisateur et timestamp
- ✅ Export tous todos utilisateur
- ✅ Gestion des erreurs et retry

### 🔄 Synchronisation JSONPlaceholder
- ✅ Synchronisation automatique via Kafka queue
- ✅ Mapping todos externes vers base locale
- ✅ Gestion des conflits et duplicatas

### 🔔 Notifications Temps Réel
- ✅ WebSocket avec STOMP protocol
- ✅ Notifications création/modification todos
- ✅ Notifications completion tâches
- ✅ Notifications privées par utilisateur

### 📊 Infrastructure
- ✅ PostgreSQL avec migration Flyway
- ✅ Redis pour cache et sessions
- ✅ Apache Kafka pour messaging
- ✅ Docker Compose pour développement
- ✅ Configuration profils (dev/prod)

---

### 🎉 **Phase 3 (Sprint 5-6) - Authentification, Architecture & Synchronisation** ✅ TERMINÉE

#### 3.1 ✅ Sécurité & Authentification (Frontend)
- [x] **Correction Interceptor JWT** - Enregistrement via `withInterceptorsFromDi()`
- [x] **Service Auth Singleton** - Instance unique pour gestion d'état cohérente
- [x] **Mise à jour Passwords** - Hashs BCrypt corrigés via Flyway `V2`

#### 3.2 ✅ Architecture Frontend (Angular 19)
- [x] **Restructuration Modulaire** - Séparation `AppComponent` et `TodosComponent` (Standalone)
- [x] **Routage Avancé** - Protection avec `AuthGuard` et `AdminGuard`
- [x] **Shell UI** - Mise en place d'un layout moderne et extensible

#### 3.3 ✅ Synchronisation & Backend
- [x] **Mapping External IDs** - Colonne `external_id` pour éviter les conflits de PK
- [x] **Optimisation Performance** - Correction Optimistic Locking lors de la sync
- [x] **Vue Globale Admin** - Visibilité des 200 todos synchronisés pour les administrateurs

#### 3.4 ✅ Expérience Utilisateur (UX)
- [x] **Pagination Frontend** - Système complet de navigation (Précédent/Suivant, numéros de page)
- [x] **Dashboard Dynamique** - Statistiques globales en temps réel
- [x] **Intégration Design** - UI responsive avec pagination professionnelle

---

## 🚧 **Phase 4 (Sprint 7-8) - Fonctionnalités Métier Avancées & UX Moderne**

### 🎯 Fonctionnalités Métier Avancées
- [ ] Catégories de tâches avec couleurs
- [ ] Dates d'échéance avec notifications automatiques
- [ ] Priorités (HAUTE, MOYENNE, BASSE)
- [ ] Commentaires et historique des modifications
- [ ] Assignation multi-utilisateurs

### 🎨 Interface Utilisateur Moderne
- [ ] Migration Angular Material Design
- [ ] Interface responsive mobile-first
- [ ] Mode sombre/clair
- [ ] Drag & Drop pour réorganisation
- [ ] Vue Kanban en plus de la liste

### 📈 Dashboard et Analytics
- [ ] Statistiques de productivité avancées
- [ ] Graphiques de progression Chart.js
- [ ] Rapports périodiques automatisés
- [ ] Notifications push navigateur

### 4.1 📱 Progressive Web App (PWA)
- [ ] **Service Worker** - Cache offline et sync background
- [ ] **Manifest Web App** - Installation mobile native-like
- [ ] **Offline capabilities** - Fonctionnement sans réseau
- [ ] **Background sync** - Synchronisation automatique en arrière-plan

---

## 🛠️ Architecture Technique Implémentée

### Backend Spring Boot
```
📦 be.cm.todoapplication
 ┣ 📂 config/           # Configuration (Security, Kafka, WebSocket, Async)
 ┣ 📂 controller/       # REST Controllers (Auth, Todo, WebSocket)
 ┣ 📂 dto/              # Data Transfer Objects (Auth, Todo, Messaging)
 ┣ 📂 exception/        # Gestion globale des erreurs
 ┣ 📂 model/            # Entités JPA (User, Todo)
 ┣ 📂 repository/       # Repositories Spring Data JPA
 ┣ 📂 service/          # Services métier (Auth, Todo, PDF, Kafka, WebSocket)
 └ 📂 service/messaging # Services Kafka spécialisés
```

### Configuration Docker
```yaml
services:
  postgres:      # Base de données principale
  kafka:         # Message broker
  zookeeper:     # Kafka dependency
  redis:         # Cache et sessions
  todo-app:      # Application Spring Boot
```

### Topics Kafka
- `pdf-processing` - Génération asynchrone PDFs
- `todo-sync` - Synchronisation JSONPlaceholder  
- `notifications` - Notifications WebSocket

---

## 🚦 Guide de Démarrage

### Prérequis
- Java 17+
- PostgreSQL 15+
- Docker & Docker Compose
- Node.js 18+ (pour Angular)

### Démarrage Rapide
```bash
# 1. Cloner et naviguer
cd TodosPdfSignedApplication

# 2. Démarrer l'infrastructure
docker-compose up -d

# 3. Démarrer l'application
cd TodoApplication
mvn spring-boot:run

# 4. Tester l'API
curl http://localhost:8080/actuator/health
```

### Test des Fonctionnalités
```bash
# 1. Inscription
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 2. Connexion
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# 3. Créer un Todo (avec token JWT)
curl -X POST http://localhost:8080/api/todos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Mon premier todo","description":"Test","completed":false}'

# 4. Générer PDF asynchrone
curl -X POST http://localhost:8080/api/todos/export/pdf \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🏆 Métriques de Succès Actuelles

| Métrique | Objectif | Actuel | Statut |
|----------|----------|---------|---------|
| Authentification JWT | Fonctionnel | ✅ Intercepteurs OK | ✅ |
| Base PostgreSQL | Migration complète | ✅ Flyway OK | ✅ |
| Kafka Messaging | 3 topics + consumers | ✅ Opérationnel | ✅ |
| WebSocket temps réel | Notifications | ✅ STOMP + SockJS | ✅ |
| Sync JSONPlaceholder | 200 records | ✅ External ID Map | ✅ |
| UX & Navigation | Pagination | ✅ Implémenté | ✅ |
| Redis Cache | Configuration | ✅ Intégré | ✅ |

---

## 📋 Checklist de Sécurité Implémentée

- [x] ✅ Validation côté serveur pour tous les inputs
- [x] ✅ JWT avec expiration et signature sécurisée
- [x] ✅ Headers de sécurité HTTP (CORS, etc.)
- [x] ✅ Hashage bcrypt des mots de passe
- [x] ✅ Protection des endpoints sensibles
- [x] ✅ Validation des rôles utilisateur
- [x] ✅ Audit logs des actions (created_by, updated_by)

---

## 📞 Support et Contribution

### Environnement de Développement
- **Port Backend** : 8080
- **PostgreSQL** : 5432
- **Kafka** : 9092  
- **Redis** : 6379
- **WebSocket** : ws://localhost:8080/ws

### Logs et Monitoring
```bash
# Logs application
docker logs todo-app -f

# Logs Kafka
docker logs kafka -f

# Monitoring topics Kafka
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

**Prochaines étapes recommandées :**
1. ✅ **Phases 1, 2 & 3 COMPLÈTÉES** - Architecture et bases fonctionnelles robustes
2. 🎯 **Phase 4** - Fonctionnalités métier avancées et UX moderne
3. 🚀 **Phase 5** - PWA, IA et intégrations externes

L'application dispose maintenant d'une **architecture moderne et scalable** avec messaging asynchrone, notifications temps réel et synchronisation externe robuste ! 🚀

## 🎯 **Phase 3 (Sprint 5-6) - En Cours** 🚧

### 3.1 🔄 Fonctionnalités Métier Avancées
- [ ] **Catégories de tâches** - Modèle Category + relation ManyToOne
- [ ] **Dates d'échéance** - Champ dueDate + notifications automatiques
- [ ] **Priorités** - Enum Priority (HAUTE, MOYENNE, BASSE)
- [ ] **Commentaires** - Entité Comment + historique modifications
- [ ] **Assignation multi-utilisateurs** - Relation ManyToMany User-Todo

```java
// Extensions modèle à implémenter
@Entity
public class Todo {
    // ...existing fields...
    @Enumerated(EnumType.STRING)
    private Priority priority;
    
    @Column
    private LocalDateTime dueDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();
    
    @ManyToMany
    @JoinTable(name = "todo_assignees")
    private Set<User> assignees = new HashSet<>();
}
```

### 3.2 🎨 Interface Utilisateur Moderne
- [ ] **Angular Material** - Migration complète du design system
- [ ] **Mode sombre/clair** - Theme switcher avec persistance
- [ ] **Responsive mobile-first** - Optimisation écrans mobiles
- [ ] **Drag & Drop** - CDK Angular pour réorganisation
- [ ] **Vue Kanban** - Colonnes par statut (Todo/En cours/Terminé)

### 3.3 📊 Dashboard et Analytics
- [ ] **Statistiques productivité** - Graphiques completion rate
- [ ] **Métriques temps réel** - WebSocket pour updates instantanés
- [ ] **Rapports périodiques** - Scheduled tasks pour génération auto
- [ ] **Graphiques interactifs** - Chart.js ou D3.js intégration

### 3.4 🔔 Notifications Avancées
- [ ] **Notifications push** - Service Worker pour notifications navigateur
- [ ] **Emails automatiques** - Spring Mail pour rappels échéances
- [ ] **Templates notifications** - Messages personnalisés par type
- [ ] **Preferences utilisateur** - Configuration types de notifications

---

## 🚀 **Phase 4 (Sprint 7-8) - Avancé**

### 4.1 📱 Progressive Web App (PWA)
- [ ] **Service Worker** - Cache offline et sync background
- [ ] **Manifest Web App** - Installation mobile native-like
- [ ] **Offline capabilities** - Fonctionnement sans réseau
- [ ] **Background sync** - Synchronisation automatique en arrière-plan

### 4.2 🤖 Intelligence Artificielle
- [ ] **Suggestions automatiques** - ML pour catégorisation tâches
- [ ] **Estimation durées** - Algorithmes prédictifs temps completion
- [ ] **Détection tâches similaires** - Recommandations basées historique
- [ ] **Priorisation intelligente** - Auto-assignment priorités

### 4.3 🔗 Intégrations Externes
- [ ] **Google Calendar** - Synchronisation bidirectionnelle événements
- [ ] **Slack/Teams** - Notifications et commandes chat
- [ ] **API REST publique** - Endpoints pour intégrations tierces
- [ ] **Webhooks** - Notifications événements vers systèmes externes

### 4.4 👥 Collaboration Avancée
- [ ] **Espaces de travail** - Workspace multi-projets
- [ ] **Permissions granulaires** - RBAC avancé par projet
- [ ] **Commentaires temps réel** - Collaboration live sur tâches
- [ ] **Mentions utilisateurs** - Notifications @user style

---

### 🧪 **5. Qualité et Tests**

#### 5.1 Tests Backend
- [ ] Tests unitaires avec JUnit 5 et Mockito
- [ ] Tests d'intégration avec TestContainers
- [ ] Tests de sécurité avec OWASP ZAP
- [ ] Coverage minimum 80%

```java
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;
    
    @InjectMocks
    private TodoService todoService;
    
    @Test
    void shouldCreateTodo() {
        // Test implementation
    }
}
```

#### 5.2 Tests Frontend
- [ ] Tests unitaires avec Jest
- [ ] Tests E2E avec Cypress
- [ ] Tests de performance avec Lighthouse
- [ ] Tests d'accessibilité (a11y)

#### 5.3 Code Quality
- [ ] SonarQube pour l'analyse statique
- [ ] ESLint/Prettier pour le frontend
- [ ] Checkstyle pour le backend
- [ ] Documentation API avec Swagger/OpenAPI

### 🔧 **6. DevOps et Déploiement**

#### 6.1 Containerisation
- [ ] Dockerfile optimisé pour le backend
- [ ] Docker Compose pour le développement
- [ ] Kubernetes manifests pour la production

```dockerfile
# Dockerfile multi-stage pour Spring Boot
FROM openjdk:17-jdk-slim AS build
COPY . /workspace
WORKDIR /workspace
RUN ./mvnw package -DskipTests

FROM openjdk:17-jre-slim
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

#### 6.2 CI/CD
- [ ] GitHub Actions ou GitLab CI
- [ ] Tests automatisés sur chaque PR
- [ ] Déploiement automatique en staging
- [ ] Rollback automatique en cas d'échec

#### 6.3 Monitoring et Observabilité
- [ ] Logs structurés avec Logback
- [ ] Métriques avec Micrometer/Prometheus
- [ ] Health checks et monitoring
- [ ] Alerting avec thresholds

### 📱 **7. Extensions Futures**

#### 7.1 Intégrations
- [ ] Synchronisation Google Calendar
- [ ] Intégration Slack/Teams
- [ ] API REST publique avec rate limiting
- [ ] Webhooks pour les événements

#### 7.2 Intelligence Artificielle
- [ ] Suggestion automatique de catégories
- [ ] Estimation automatique des durées
- [ ] Détection de tâches similaires
- [ ] Priorisation intelligente

#### 7.3 Collaboration
- [ ] Espaces de travail partagés
- [ ] Commentaires temps réel
- [ ] Mentions d'utilisateurs
- [ ] Permissions granulaires

---

## 🛠️ Guide d'implémentation par phases

### ✅ Phase 1 (Sprint 1-2) - Fondations TERMINÉE
1. ✅ Migration base de données PostgreSQL
2. ✅ Authentification JWT complète
3. ✅ Tests unitaires essentiels (JUnit 5 + Mockito)
4. ✅ Validation des données avec Bean Validation

### ✅ Phase 2 (Sprint 3-4) - Performance & Messaging TERMINÉE  
1. ✅ Cache Redis intégré
2. ✅ Pagination et optimisation requêtes
3. ✅ **Configuration Apache Kafka complète**
4. ✅ **Traitement asynchrone des PDFs**
5. ✅ **Queue pour synchronisation JSONPlaceholder**
6. ✅ **WebSockets notifications temps réel**
7. ✅ Monitoring basique avec Spring Actuator

#### Détails d'implémentation Kafka COMPLÉTÉS:

**✅ Configuration Kafka & Infrastructure**
- ✅ Installation et configuration Kafka Docker
- ✅ Configuration Spring Kafka dans `application.properties`
- ✅ Création des topics (`pdf-processing`, `todo-sync`, `notifications`)
- ✅ Configuration producers et consumers avec JSON serialization
- ✅ Tests unitaires messaging avec TestContainers

**✅ Services asynchrones opérationnels**
- ✅ Service asynchrone génération PDF avec `@Async` et Kafka
- ✅ Queue Kafka pour synchronisation JSONPlaceholder automatique
- ✅ Gestion des erreurs avec retry logic
- ✅ Monitoring topics et métriques Kafka via Actuator

```yaml
# Configuration Docker Compose opérationnelle
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
  
  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: todoapp
      POSTGRES_USER: todouser
      POSTGRES_PASSWORD: todopassword
      
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

**✅ WebSockets et notifications temps réel OPÉRATIONNELS**
- ✅ Configuration WebSocket avec STOMP + SockJS  
- ✅ Intégration WebSocket avec Kafka pour diffusion notifications
- ✅ Types notifications (création, modification, échéance tâches)
- ✅ Architecture prête pour client Angular WebSocket

```java
// Configuration WebSocket opérationnelle
@Configuration
@EnableWebSocketMessageBroker  
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
```

### ✅ Phase 3 (Sprint 5-6) - Authentification & Architecture TERMINÉE
1. ✅ Correction interceptors HttpClient
2. ✅ Routage sécurisé (Guards)
3. ✅ Standalone Components refactoring
4. ✅ Synchronisation avec `external_id`
5. ✅ Pagination UI et global view admin

### 🚧 Phase 4 (Sprint 7-8) - Fonctionnalités & UX EN COURS
1. [ ] Catégories et priorités todos
2. [ ] Dates d'échéance avec notifications automatiques
3. [ ] Interface Angular Material moderne
4. [ ] Vue Kanban et drag & drop
5. [ ] Notifications push navigateur

### 🎯 Phase 5 (Sprint 9-10) - Avancé PLANIFIÉ
1. [ ] Progressive Web App (PWA)
2. [ ] Intelligence artificielle (suggestions)
3. [ ] Intégrations externes (Google Calendar, Slack)
4. [ ] Analytics et rapports automatisés
5. [ ] Tests E2E complets avec Cypress

---

## 📋 Checklist de Sécurité

- [ ] Validation côté serveur pour tous les inputs
- [ ] Protection CSRF avec tokens
- [ ] Headers de sécurité HTTP
- [ ] Audit logs des actions sensibles
- [ ] Chiffrement des données sensibles
- [ ] Rate limiting sur les APIs
- [ ] Scan de vulnérabilités régulier

---

## 🏆 Métriques de Succès

| Métrique | Objectif | Actuel |
|----------|----------|---------|
| Temps de réponse API | < 200ms | ~500ms |
| Coverage tests | > 80% | 0% |
| Score Lighthouse | > 90 | ~60 |
| Vulnérabilités | 0 critique | Non évalué |
| Uptime | 99.9% | Non suivi |

---

## 🔗 Resources Utiles

- [Spring Boot Best Practices](https://spring.io/guides)
- [Angular Style Guide](https://angular.io/guide/styleguide)
- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/)
- [12-Factor App](https://12factor.net/)

---

## 📞 Support et Contribution

Pour toute question ou suggestion d'amélioration, créer une issue dans le repository.

**Prochaines étapes recommandées :**
1. Prioriser les améliorations selon les besoins métier
2. Créer des user stories détaillées
3. Estimer l'effort de développement
4. Planifier les sprints de développement

## 🚀 Run Application (Spring Boot and Angular)

### 📋 Prérequis

#### Logiciels requis
- **Java 17+** - OpenJDK ou Oracle JDK
- **Maven 3.8+** - Pour la compilation Spring Boot
- **Node.js 18+** - Pour Angular et npm
- **PostgreSQL 15+** - Base de données principale
- **Docker & Docker Compose** - Pour l'infrastructure (optionnel)

#### Vérification des prérequis
```bash
# Vérifier Java
java --version
# Attendu: openjdk 17.x.x ou plus

# Vérifier Maven
mvn --version
# Attendu: Apache Maven 3.8.x ou plus

# Vérifier Node.js
node --version
# Attendu: v18.x.x ou plus

# Vérifier npm
npm --version
# Attendu: 8.x.x ou plus

# Vérifier PostgreSQL
psql --version
# Attendu: psql (PostgreSQL) 15.x ou plus
```

---

### 🐳 Option 1 : Démarrage avec Docker Compose (Recommandé)

#### 1. Démarrer l'infrastructure complète
```bash
# Cloner le projet si pas encore fait
git clone <repository-url>
cd TodosPdfSignedApplication

# Démarrer tous les services (PostgreSQL, Kafka, Redis, App)
docker-compose up -d

# Vérifier le statut des conteneurs
docker-compose ps
```

#### 2. Vérifier les services
```bash
# Vérifier les logs de l'application
docker-compose logs -f todo-app

# Vérifier PostgreSQL
docker exec -it postgres psql -U todouser -d todoapp -c "\dt"

# Vérifier Kafka topics
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Vérifier Redis
docker exec -it redis redis-cli ping
```

#### 3. Tester l'application
```bash
# Health check
curl http://localhost:8080/actuator/health

# Test API
curl http://localhost:8080/api/todos
```

---

### 🛠️ Option 2 : Démarrage local (Développement)

#### 1. Configurer PostgreSQL local
```bash
# Créer la base de données
createdb -U postgres todoapp

# Créer l'utilisateur
psql -U postgres -c "CREATE USER todouser WITH PASSWORD 'todopassword';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE todoapp TO todouser;"
psql -U postgres -c "GRANT ALL ON SCHEMA public TO todouser;"

# Tester la connexion
psql -U todouser -d todoapp -c "SELECT current_database();"
```

#### 2. Démarrer le Backend Spring Boot
```bash
# Naviguer vers le dossier backend
cd TodoApplication

# Compiler le projet
mvn clean compile

# Option A: Démarrage avec profil simple (sans Kafka/Redis)
mvn spring-boot:run -Dspring-boot.run.profiles=simple

# Option B: Démarrage complet (nécessite Kafka et Redis démarrés)
mvn spring-boot:run

# Option C: Package et exécution du JAR
mvn clean package -DskipTests
java -jar target/TodoApplication-0.0.1-SNAPSHOT.jar
```

#### 3. Démarrer le Frontend Angular
```bash
# Naviguer vers le dossier frontend
cd ../TodoApp

# Installer les dépendances
npm install

# Démarrer le serveur de développement
ng serve

# Ou avec port spécifique
ng serve --port 4200 --open
```

#### 4. Démarrer Kafka et Redis (si profil complet)
```bash
# Option A: Avec Docker Compose seulement l'infrastructure
docker-compose up -d postgres kafka zookeeper redis

# Option B: Installation locale
# Kafka
cd /path/to/kafka
bin/kafka-server-start.sh config/server.properties

# Redis
redis-server

# Zookeeper (pour Kafka)
bin/zookeeper-server-start.sh config/zookeeper.properties
```

---

### ⚡ Option 3 : Démarrage rapide simplifié

#### Pour tester rapidement sans toutes les dépendances
```bash
# 1. PostgreSQL uniquement avec Docker
docker run -d \
  --name postgres-todo \
  -e POSTGRES_DB=todoapp \
  -e POSTGRES_USER=todouser \
  -e POSTGRES_PASSWORD=todopassword \
  -p 5432:5432 \
  postgres:15

# 2. Backend avec profil simplifié
cd TodoApplication
mvn spring-boot:run -Dspring-boot.run.profiles=simple

# 3. Frontend (terminal séparé)
cd TodoApp
npm install && ng serve
```

---

### 🔧 Configuration des profils

#### Profils disponibles
- **`simple`** - PostgreSQL uniquement, sans Kafka/Redis
- **`dev`** - Développement complet avec toutes les fonctionnalités
- **`prod`** - Production avec optimisations

#### Variables d'environnement optionnelles
```bash
# Pour personnaliser la configuration
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/todoapp
export SPRING_DATASOURCE_USERNAME=todouser
export SPRING_DATASOURCE_PASSWORD=todopassword
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_REDIS_HOST=localhost
export JWT_SECRET=myCustomSecretKey
```

---

### 📊 Ports utilisés par défaut

| Service | Port | URL | Description |
|---------|------|-----|-------------|
| **Spring Boot API** | 8080 | http://localhost:8080 | Backend REST API |
| **Angular Frontend** | 4200 | http://localhost:4200 | Interface utilisateur |
| **PostgreSQL** | 5432 | localhost:5432 | Base de données |
| **Kafka** | 9092 | localhost:9092 | Message broker |
| **Zookeeper** | 2181 | localhost:2181 | Kafka coordination |
| **Redis** | 6379 | localhost:6379 | Cache en mémoire |
| **Kafka UI** | 8090 | http://localhost:8090 | Interface Kafka |
| **Redis Commander** | 8091 | http://localhost:8091 | Interface Redis |

---

### ✅ Vérifications après démarrage

#### 1. Backend Spring Boot
```bash
# Health check
curl http://localhost:8080/actuator/health

# Info application
curl http://localhost:8080/actuator/info

# Métriques
curl http://localhost:8080/actuator/metrics
```

#### 2. Base de données
```bash
# Vérifier les tables créées par Flyway
psql -U todouser -d todoapp -c "\dt"

# Vérifier les données initiales
psql -U todouser -d todoapp -c "SELECT * FROM users;"
```

#### 3. Frontend Angular
- Naviguer vers http://localhost:4200
- Vérifier que la page de connexion s'affiche
- Tester l'inscription d'un utilisateur

#### 4. API Tests complets
```bash
# Inscription utilisateur
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Connexion
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Utiliser le token reçu pour créer un todo
curl -X POST http://localhost:8080/api/todos \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Mon premier todo","description":"Description test","completed":false}'
```

---

### 🐛 Dépannage courant

#### Problème : Base de données inaccessible
```bash
# Vérifier si PostgreSQL fonctionne
pg_isready -h localhost -p 5432

# Redémarrer PostgreSQL (Windows)
net restart postgresql-x64-15

# Vérifier les connexions
netstat -an | findstr 5432
```

#### Problème : Port déjà utilisé
```bash
# Trouver quel processus utilise le port 8080
netstat -ano | findstr :8080
tasklist /FI "PID eq <PID_NUMBER>"

# Arrêter le processus ou changer le port
# Dans application.properties: server.port=8081
```

#### Problème : Erreurs Lombok
```bash
# Utiliser le profil simple qui contourne les problèmes Lombok
mvn spring-boot:run -Dspring-boot.run.profiles=simple

# Ou nettoyer et recompiler
mvn clean compile -U
```

#### Problème : Docker
```bash
# Vérifier si Docker fonctionne
docker --version
docker info

# Redémarrer Docker Desktop si nécessaire
# Ou utiliser docker-compose-simple.yml
docker-compose -f docker-compose-simple.yml up -d
```

---

### 🐳 Guide Docker Compose Détaillé

#### 1. Structure des fichiers Docker Compose disponibles
```
├── docker-compose.yml           # Stack complète (recommandé)
├── docker-compose-simple.yml    # PostgreSQL uniquement
└── docker-compose-prod.yml      # Configuration production
```

#### 2. Démarrage de la stack complète
```bash
# Démarrer tous les services en arrière-plan
docker-compose up -d

# Démarrer avec reconstruction des images
docker-compose up -d --build

# Voir les logs en temps réel
docker-compose logs -f

# Voir les logs d'un service spécifique
docker-compose logs -f todo-app
docker-compose logs -f postgres
docker-compose logs -f kafka
```

#### 3. Services inclus dans docker-compose.yml
```yaml
services:
  # Base de données principale
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: todoapp
      POSTGRES_USER: todouser
      POSTGRES_PASSWORD: todopassword
    volumes: [postgres-data:/var/lib/postgresql/data]
    
  # Coordination Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    ports: ["2181:2181"]
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      
  # Message broker
  kafka:
    image: confluentinc/cp-kafka:latest
    ports: ["9092:9092", "29092:29092"]
    depends_on: [zookeeper]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      
  # Cache en mémoire
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --appendonly yes
    volumes: [redis-data:/data]
    
  # Interface Kafka (optionnel)
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    ports: ["8090:8080"]
    depends_on: [kafka]
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      
  # Interface Redis (optionnel)
  redis-commander:
    image: rediscommander/redis-commander:latest
    ports: ["8091:8081"]
    depends_on: [redis]
    environment:
      REDIS_HOSTS: local:redis:6379
      
  # Application Spring Boot
  todo-app:
    build:
      context: ./TodoApplication
      dockerfile: Dockerfile
    ports: ["8080:8080"]
    depends_on:
      postgres: {condition: service_healthy}
      kafka: {condition: service_started}
      redis: {condition: service_started}
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/todoapp
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_DATA_REDIS_HOST: redis
    volumes:
      - todo-storage:/app/storage
      - todo-pdf-storage:/app/pdf-storage
```

#### 4. Commandes Docker Compose utiles
```bash
# État des services
docker-compose ps

# Redémarrer un service spécifique
docker-compose restart todo-app
docker-compose restart postgres

# Arrêter tous les services
docker-compose stop

# Arrêter et supprimer les conteneurs
docker-compose down

# Arrêter et supprimer les volumes (⚠️ perte des données)
docker-compose down -v

# Forcer la reconstruction des images
docker-compose build --no-cache

# Mise à l'échelle d'un service
docker-compose up -d --scale todo-app=2
```

#### 5. Monitoring et debugging
```bash
# Utilisation des ressources
docker stats

# Inspecter un conteneur
docker inspect postgres
docker inspect todo-app

# Se connecter à un conteneur
docker exec -it postgres psql -U todouser -d todoapp
docker exec -it redis redis-cli
docker exec -it kafka bash

# Vérifier les volumes
docker volume ls
docker volume inspect todosPdfSignedApplication_postgres-data
```

#### 6. Configuration pour différents environnements

**Développement (docker-compose.yml)**
```bash
# Démarrage standard
docker-compose up -d

# Variables d'environnement de développement
SPRING_PROFILES_ACTIVE=dev
SPRING_JPA_SHOW_SQL=true
LOG_LEVEL=DEBUG
```

**Production (docker-compose-prod.yml)**
```bash
# Démarrage production
docker-compose -f docker-compose-prod.yml up -d

# Variables d'environnement de production
SPRING_PROFILES_ACTIVE=prod
SPRING_JPA_SHOW_SQL=false
LOG_LEVEL=INFO
JWT_SECRET=${JWT_SECRET_FROM_ENV}
```

**Simple (docker-compose-simple.yml)**
```bash
# Démarrage minimal (PostgreSQL uniquement)
docker-compose -f docker-compose-simple.yml up -d

# Utile pour développement local sans Kafka/Redis
```

#### 7. Gestion des données et sauvegardes
```bash
# Sauvegarder la base de données
docker exec postgres pg_dump -U todouser todoapp > backup.sql

# Restaurer la base de données
cat backup.sql | docker exec -i postgres psql -U todouser -d todoapp

# Sauvegarder les volumes
docker run --rm -v todosPdfSignedApplication_postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .

# Restaurer les volumes
docker run --rm -v todosPdfSignedApplication_postgres-data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /data
```

#### 8. Résolution des problèmes courants

**Problème : Service ne démarre pas**
```bash
# Vérifier les logs détaillés
docker-compose logs service-name

# Vérifier l'état de santé
docker-compose ps

# Redémarrer un service problématique
docker-compose restart service-name
```

**Problème : Port déjà utilisé**
```bash
# Modifier les ports dans docker-compose.yml
ports:
  - "8081:8080"  # Au lieu de 8080:8080

# Ou arrêter le processus qui utilise le port
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Problème : Problèmes de réseau Docker**
```bash
# Recréer le réseau
docker-compose down
docker network prune
docker-compose up -d

# Vérifier la connectivité entre services
docker-compose exec todo-app ping postgres
docker-compose exec todo-app ping kafka
```

**Problème : Volumes corrompus**
```bash
# Sauvegarder les données importantes d'abord !
# Puis supprimer et recréer les volumes
docker-compose down -v
docker volume prune
docker-compose up -d
```

#### 9. Performance et optimisation
```bash
# Limiter les ressources par service
services:
  todo-app:
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '0.5'
        reservations:
          memory: 512M

# Healthchecks pour tous les services
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

#### 10. Scripts d'automatisation
```bash
# start-docker.sh - Script de démarrage automatisé
#!/bin/bash
echo "🚀 Démarrage de TodoApp avec Docker Compose..."

# Vérifier si Docker est en cours d'exécution
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker n'est pas en cours d'exécution"
    exit 1
fi

# Démarrer les services
docker-compose up -d

# Attendre que les services soient prêts
echo "⏳ Attente de la disponibilité des services..."
sleep 30

# Vérifier la santé de l'application
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Application démarrée avec succès!"
    echo "🌐 Frontend: http://localhost:4200"
    echo "🔧 Backend: http://localhost:8080"
    echo "🗄️ Kafka UI: http://localhost:8090"
    echo "🔴 Redis UI: http://localhost:8091"
else
    echo "❌ Problème de démarrage, vérifier les logs:"
    echo "docker-compose logs -f todo-app"
fi
```

**🎯 Docker Compose est maintenant entièrement configuré et documenté !**
