# Migration de H2 vers PostgreSQL

## 📋 Résumé des modifications

J'ai complété la migration de la base de données H2 vers PostgreSQL pour l'application TodoApp. Voici ce qui a été mis en place :

## 🗄️ Configuration de la base de données

### 1. PostgreSQL dans Docker Compose

```yaml
postgres:
  image: postgres:15-alpine
  hostname: postgres
  container_name: postgres
  ports:
    - "5432:5432"
  environment:
    POSTGRES_DB: todoapp
    POSTGRES_USER: todouser
    POSTGRES_PASSWORD: todopassword
  volumes:
    - postgres-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U todouser -d todoapp"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### 2. Configuration Spring Boot

#### application.properties (Développement local)
```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/todoapp
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=todouser
spring.datasource.password=todopassword

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

#### application-prod.properties (Production)
```properties
# PostgreSQL Database Configuration (utilise le service docker)
spring.datasource.url=jdbc:postgresql://postgres:5432/todoapp
spring.datasource.username=todouser
spring.datasource.password=todopassword
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration pour PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Flyway pour les migrations
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 3. Dépendances Maven

Le fichier `pom.xml` contient déjà la dépendance PostgreSQL :

```xml
<!-- PostgreSQL pour la production -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 4. Migration Flyway

Le script de migration `V1__Initial_schema.sql` est déjà compatible PostgreSQL :

```sql
-- Table des utilisateurs
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

-- Table des todos
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
```

## 🚀 Démarrage de l'environnement

### 1. Avec Docker Compose (Recommandé)

```powershell
# Démarrer tous les services (PostgreSQL, Kafka, Redis, Application)
.\start-postgres.ps1

# Ou manuellement
docker-compose up --build -d

# Vérifier le statut
docker-compose ps

# Voir les logs
docker-compose logs -f todo-app
```

### 2. Développement local (PostgreSQL externe)

Si vous avez PostgreSQL installé localement :

```bash
# Créer la base de données
createdb -U postgres todoapp

# Créer l'utilisateur
psql -U postgres -c "CREATE USER todouser WITH PASSWORD 'todopassword';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE todoapp TO todouser;"

# Démarrer l'application Spring Boot
mvn spring-boot:run
```

## 🔧 Services disponibles

- **Application TodoApp** : http://localhost:8080
- **PostgreSQL** : localhost:5432
  - Base : `todoapp`
  - Utilisateur : `todouser`
  - Mot de passe : `todopassword`
- **Kafka UI** : http://localhost:8090
- **Redis Commander** : http://localhost:8091

## 🗄️ Connexion à PostgreSQL

```bash
# Via Docker
docker exec -it postgres psql -U todouser -d todoapp

# Localement
psql -h localhost -U todouser -d todoapp
```

## ✅ Corrections apportées

### 1. Modèles Lombok
- Corrigé `User` avec `@Getter/@Setter` au lieu de `@Data` pour éviter les conflits avec `UserDetails`
- Ajouté les méthodes `getUsername()` et `getPassword()` requises par `UserDetails`
- Ajouté `@Builder` aux DTOs de messaging pour les constructeurs flexibles

### 2. Configuration Redis
- Mis à jour les propriétés dépréciées :
  - `spring.redis.host` → `spring.data.redis.host`
  - `spring.redis.port` → `spring.data.redis.port`
  - `spring.redis.timeout` → `spring.data.redis.timeout`

### 3. Service Docker
- Ajouté le service `todo-app` dans docker-compose.yml
- Configuration des dépendances avec health checks
- Volumes pour le stockage persistant des fichiers et PDFs

## 🔄 Migration des données existantes

Si vous avez des données H2 à migrer :

1. **Exporter depuis H2** :
```sql
-- Via H2 Console (http://localhost:8080/h2-console)
SCRIPT TO 'backup.sql';
```

2. **Adapter et importer dans PostgreSQL** :
```bash
# Adapter le script pour PostgreSQL (remplacer les types de données si nécessaire)
# Puis importer
psql -U todouser -d todoapp -f backup_adapted.sql
```

## 📦 Dockerfile pour l'application

Un `Dockerfile` a été créé pour containeriser l'application :

```dockerfile
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests
RUN mkdir -p /app/storage /app/pdf-storage
EXPOSE 8080
CMD ["java", "-jar", "target/TodoApplication-0.0.1-SNAPSHOT.jar"]
```

La migration vers PostgreSQL est maintenant complète et l'application est prête pour la production avec une base de données robuste et scalable.
