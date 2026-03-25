# Estágio 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime (Segurança e Leveza)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Adicionando um usuário não-root por segurança (Diretriz do The Sentinel)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
