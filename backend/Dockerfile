# Étape 1 : build du JAR
FROM maven:eclipse-temurin AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests

# Étape 2 : runtime Java propre
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]