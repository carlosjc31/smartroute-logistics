# Estágio 1: build
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# Estágio 2: runtime só com JRE, usuário sem privilégios
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system --gid 10001 spring && useradd --system --uid 10001 --gid spring --no-create-home spring
WORKDIR /app
COPY --from=build --chown=root:root /app/target/*.jar app.jar
USER spring
EXPOSE 8080
# /tmp é o único diretório gravável (tmpfs no compose); o container roda com read_only
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.io.tmpdir=/tmp", "-jar", "app.jar"]
