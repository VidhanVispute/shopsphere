# ── Stage 1: Build ──────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent POM and shared modules first for better layer caching
COPY pom.xml .
COPY shared/common-dtos ./shared/common-dtos
COPY shared/common-exceptions ./shared/common-exceptions
COPY shared/common-utils ./shared/common-utils
COPY shared/common-events ./shared/common-events

# Install shared modules into local Maven repo
RUN mvn install -f pom.xml -N -DskipTests -q
RUN mvn install -f shared/common-dtos/pom.xml -DskipTests -q
RUN mvn install -f shared/common-exceptions/pom.xml -DskipTests -q
RUN mvn install -f shared/common-utils/pom.xml -DskipTests -q
RUN mvn install -f shared/common-events/pom.xml -DskipTests -q

# Copy and build user-service
COPY services/user-service ./services/user-service
RUN mvn package -f services/user-service/pom.xml -DskipTests -q

# Extract layered JAR
WORKDIR /build/services/user-service/target
RUN java -Djarmode=layertools \
    -jar user-service-1.0.0-SNAPSHOT.jar extract

# ── Stage 2: Runtime ────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere
USER shopsphere

WORKDIR /app

COPY --from=builder /build/services/user-service/target/dependencies/ ./
COPY --from=builder /build/services/user-service/target/spring-boot-loader/ ./
COPY --from=builder /build/services/user-service/target/snapshot-dependencies/ ./
COPY --from=builder /build/services/user-service/target/application/ ./

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider \
    http://localhost:8082/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]