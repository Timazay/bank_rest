# Многоступенчатая сборка
# Стадия 1: Сборка приложения
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Копируем pom.xml для кэширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN mvn clean package -DskipTests

# Стадия 2: Запуск приложения
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем необходимые утилиты для работы
RUN apk add --no-cache curl

# Создаем пользователя для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копируем собранный JAR файл из стадии сборки
COPY --from=builder /app/target/*.jar app.jar

# Создаем директорию для логов
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Переключаемся на непривилегированного пользователя
USER appuser

# Открываем порт
EXPOSE 8080

# Точка входа
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=UTC", \
    "-Xmx512m", \
    "-Xms256m", \
    "-jar", \
    "app.jar"]