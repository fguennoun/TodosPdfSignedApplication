# Utilise OpenJDK 17 avec Alpine Linux pour une image légère
FROM openjdk:17-jdk-alpine

# Définit le répertoire de travail
WORKDIR /app

# Copie les fichiers maven pour le cache des dépendances
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./

# Rend mvnw exécutable
RUN chmod +x ./mvnw

# Télécharge les dépendances (mise en cache Docker)
RUN ./mvnw dependency:go-offline -B

# Copie le code source
COPY src src

# Compile l'application
RUN ./mvnw package -DskipTests

# Crée les répertoires de stockage
RUN mkdir -p /app/storage /app/pdf-storage

# Expose le port 8080
EXPOSE 8080

# Lance l'application
CMD ["java", "-jar", "target/TodoApplication-0.0.1-SNAPSHOT.jar"]
