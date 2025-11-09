# Étape 1 : build du JAR dans une image à part pour alléger l'image final.
FROM maven:eclipse-temurin AS build
WORKDIR /app
# pour ne pas retélécharger toutes les dépendances à chaque build on utilise dependcy:go-offline
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY . .
RUN mvn -q clean package -DskipTests

# Étape 2 : runtime Java propre
FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
