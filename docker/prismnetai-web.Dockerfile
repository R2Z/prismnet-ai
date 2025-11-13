# Multi-stage build to produce a small runtime image

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

# Build application
COPY src ./src
RUN mvn -q -e -DskipTests package


FROM eclipse-temurin:17-jre AS runtime

ENV JAVA_OPTS=""
ENV SERVER_PORT=8080

ENV OPENAI_API_KEY="xx"
ENV OPENROUTER_API_KEY="xx"
ENV ANTHROPIC_API_KEY="xx"

ENV PRISMNETAI_ADMIN_CLIENT="admin"

WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Metadata: logical network this service should be attached to when run via
# docker-compose (Dockerfiles can't force network assignment at build time).
LABEL com.prismnetai.network=prismnetai-network
