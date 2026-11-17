# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime-base
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system curralzap \
    && adduser --system --ingroup curralzap curralzap

COPY docker/entrypoint.sh /app/entrypoint.sh
COPY --from=builder /workspace/target/*.jar /app/app.jar

RUN chmod +x /app/entrypoint.sh \
    && chown -R curralzap:curralzap /app

USER curralzap
ENTRYPOINT ["/app/entrypoint.sh"]

FROM runtime-base AS api-runtime
ENV CURRALZAP_RUNTIME_ROLE=api
EXPOSE 8080

FROM runtime-base AS worker-runtime
ENV CURRALZAP_RUNTIME_ROLE=worker
