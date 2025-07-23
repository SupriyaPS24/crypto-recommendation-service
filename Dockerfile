# ----------- STAGE 1: Build the application -------------
FROM maven:3.9.4-eclipse-temurin-17-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package
# can include -DskipTests for quick startup of application (eg: RUN mvn clean package -DskipTests)
# ----------- STAGE 2: Create runtime image -------------
FROM openjdk:17-alpine

WORKDIR /app

COPY --from=builder /app/target/crypto-recommendation-service-0.0.1-SNAPSHOT.jar app.jar

COPY src/main/resources/data /app/src/main/resources/data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
