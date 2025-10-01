# ===== STAGE 1: BUILD =====
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copie les descripteurs d'abord pour profiter du cache
COPY pom.xml mvnw ./
COPY .mvn .mvn
# Pré-télécharge les dépendances (cache)
RUN ./mvnw -q -B -DskipTests dependency:go-offline

# Copie le reste puis build
COPY src src
RUN ./mvnw -q -B -DskipTests package

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copie le jar produit (adapte le nom si nécessaire)
COPY --from=build /workspace/target/*.jar /app/app.jar

# Bonnes pratiques JVM dans un conteneur
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0"

EXPOSE 8080
USER 1000:1000
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
