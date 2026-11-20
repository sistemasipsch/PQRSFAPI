# ============================================================================
# Backend PQRSF SIAU · imagen de producción
#   Etapa 1: compila el .jar con Maven (no requiere Maven/Java en el VPS)
#   Etapa 2: imagen liviana solo con el JRE 21, usuario sin privilegios
# ============================================================================

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom: las dependencias quedan en caché mientras el pom no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package


FROM eclipse-temurin:21-jre

ENV TZ=America/Bogota \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Bogota -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true" \
    UPLOAD_DIR=/app/uploads \
    LOG_FILE=/app/logs/backend.log

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=build /build/target/pqrsf-siau-backend-1.0.0.jar app.jar
RUN mkdir -p /app/uploads /app/logs && chown -R app:app /app

USER app
EXPOSE 8080

# Adjuntos de los ciudadanos y registro del servidor (montar como volúmenes)
VOLUME ["/app/uploads", "/app/logs"]

# Sano = el puerto 8080 acepta conexiones
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD bash -c '</dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
