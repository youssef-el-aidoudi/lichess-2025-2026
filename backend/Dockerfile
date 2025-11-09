# Étape 1 : build du JAR dans une image à part pour alléger l'image final.
FROM maven:3.9.9-eclipse-temurin AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests

# Étape 2 : runtime Java propre
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
