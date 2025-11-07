FROM maven:3.9.9-eclipse-temurin AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*-shaded.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
