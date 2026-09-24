# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Run stage ----------
FROM eclipse-temurin:21-jre

# curl (used by the HEALTHCHECK) ships with the base image. Installing it with apt
# made every build depend on Ubuntu's mirrors being in sync, and builds failed
# whenever a newer curl was indexed but not yet downloadable. Fail fast if it's gone.
# Run as an unprivileged user.
RUN command -v curl >/dev/null \
    && groupadd --system app \
    && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

# Size the heap from the container memory limit and restart cleanly on OOM
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT:-8080}/api/health/liveness" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
