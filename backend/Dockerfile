# Étape 1 : build du JAR
FROM maven:eclipse-temurin AS build
WORKDIR /app
COPY . .
#RUN mvn -q clean package -DskipTests
RUN mvn -q clean package -Dmaven.test.skip=true

# Étape 2 : runtime Java propre avec Stockfish
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && \
    apt-get install -y docker.io curl && \
    mkdir -p /usr/lib/docker/cli-plugins && \
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
    -o /usr/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/lib/docker/cli-plugins/docker-compose && \
    rm -rf /var/lib/apt/lists/*

# Installer Stockfish
RUN apt-get update && apt-get install -y stockfish && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]