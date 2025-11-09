FROM maven:3.9.9-eclipse-temurin AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ./data /app/data
COPY .env /app/.env
COPY --from=build /app/target/etl-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
