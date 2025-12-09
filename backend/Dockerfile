

# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source code and build
COPY . .
RUN mvn -q clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Stockfish from package manager
RUN apt-get update && apt-get install -y stockfish && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy the compiled JAR from build stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app.jar

EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
