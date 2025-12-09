
FROM maven:3.9.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/etl-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
