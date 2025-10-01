FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/app.jar
COPY ${JAR_FILE} /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
