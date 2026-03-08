# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy parent pom first — lets Docker cache the layer if only source changes
COPY pom.xml .
COPY services/auth-service/pom.xml services/auth-service/pom.xml

# Copy shared modules (auth-service depends on all four)
COPY shared/common-dtos/pom.xml         shared/common-dtos/pom.xml
COPY shared/common-exceptions/pom.xml   shared/common-exceptions/pom.xml
COPY shared/common-utils/pom.xml        shared/common-utils/pom.xml
COPY shared/common-events/pom.xml       shared/common-events/pom.xml

# Download dependencies — cached unless a pom.xml changes
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -f pom.xml -N -q && \
    mvn dependency:go-offline -f shared/common-dtos/pom.xml -q && \
    mvn dependency:go-offline -f shared/common-exceptions/pom.xml -q && \
    mvn dependency:go-offline -f shared/common-utils/pom.xml -q && \
    mvn dependency:go-offline -f shared/common-events/pom.xml -q && \
    mvn dependency:go-offline -f services/auth-service/pom.xml -q

# Copy source
COPY shared/common-dtos/src         shared/common-dtos/src
COPY shared/common-exceptions/src   shared/common-exceptions/src
COPY shared/common-utils/src        shared/common-utils/src
COPY shared/common-events/src       shared/common-events/src
COPY services/auth-service/src      services/auth-service/src

# Build shared libs, then service
RUN mvn install -f pom.xml -N -DskipTests -q && \
    mvn install -f shared/common-dtos/pom.xml -DskipTests -q && \
    mvn install -f shared/common-exceptions/pom.xml -DskipTests -q && \
    mvn install -f shared/common-utils/pom.xml -DskipTests -q && \
    mvn install -f shared/common-events/pom.xml -DskipTests -q && \
    mvn package -f services/auth-service/pom.xml -DskipTests -q

# ── Stage 2: Layer extraction ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS extractor

WORKDIR /app

COPY --from=builder /build/services/auth-service/target/*.jar app.jar

# Spring Boot layertools splits the fat jar into:
#   dependencies / spring-boot-loader / snapshot-dependencies / application
# This means only the "application" layer rebuilds on code changes — not all deps
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 3: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Non-root user — never run as root in production
RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere

WORKDIR /app

# Copy layers in dependency-stability order (most stable → least stable)
# Docker cache hits on the first three layers unless deps actually change
COPY --from=extractor /app/dependencies          ./dependencies
COPY --from=extractor /app/spring-boot-loader    ./spring-boot-loader
COPY --from=extractor /app/snapshot-dependencies ./snapshot-dependencies
COPY --from=extractor /app/application           ./application

RUN chown -R shopsphere:shopsphere /app
USER shopsphere

EXPOSE 8081

# Container-aware JVM flags:
#   UseContainerSupport  — reads cgroup memory limits instead of host RAM
#   MaxRAMPercentage     — uses 75% of the container's allocated memory
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]