# Stage 1: Загрузка зависимостей
FROM maven:3.9.5-eclipse-temurin-17 AS deps

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

# Stage 2: Сборка
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

COPY --from=deps /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=UTC", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", \
    "app.jar"]