





# Use pre-built JAR from host
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Stockfish from package manager
RUN apt-get update && apt-get install -y stockfish && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy pre-built JAR from target directory
COPY target/demo-0.0.1-SNAPSHOT.jar /app.jar

EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
