FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -Dquarkus.package.type=uber-jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*-runner.jar /app/application.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]