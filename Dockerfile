# Build stage: compile and package with the Maven wrapper (tests run in CI / locally, not here)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Run stage: just the JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/hmp-admin-api-*.jar app.jar
# Render sets $PORT; application.properties reads it (default 8080).
# Out of memory: stop the JVM so Render restarts it, instead of running on with a broken in-memory database
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
