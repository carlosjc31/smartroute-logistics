# Estágio 1: Build
FROM openjdk:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime (Segurança e Leveza)
FROM openjdk:17-jre-alpine
WORKDIR /app
# Adicionando um usuário não-root por segurança (Diretriz do The Sentinel)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
