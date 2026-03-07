# =============================================================================
# Multi-stage build for Config Server
#
# Stage 1 (builder): Extracts the layered JAR contents
# Stage 2 (runtime): Assembles the final minimal image
#
# WHY multi-stage?
# The builder stage needs JDK tools to extract the JAR.
# The runtime stage only needs JRE to run the app.
# Without multi-stage, you'd ship the JDK in production — 
# that's 3x larger image and a larger attack surface.
# =============================================================================

# Stage 1 — Extract layers from the fat JAR
FROM eclipse-temurin:17-jre-alpine AS builder

WORKDIR /app

# Copy the fat JAR built by Maven
COPY target/*.jar app.jar

# Extract the layered JAR into separate directories.
# Spring Boot's JarLauncher knows how to do this extraction.
# Result:
#   /app/extracted/dependencies/
#   /app/extracted/spring-boot-loader/
#   /app/extracted/snapshot-dependencies/
#   /app/extracted/application/
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

# =============================================================================
# Stage 2 — Build the final runtime image
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

# WHY alpine?
# eclipse-temurin:17-jre       = ~250MB
# eclipse-temurin:17-jre-alpine = ~85MB
# Alpine is a minimal Linux distro. Smaller image = faster pull = smaller
# attack surface. The tradeoff is it uses musl libc instead of glibc,
# but Spring Boot + Java works fine on it.

WORKDIR /app

# Create a non-root user to run the application.
# WHY: Running as root inside a container is a security risk.
# If the container is compromised, the attacker has root inside it.
# A dedicated user limits the blast radius.
RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere

# Copy each layer separately — Docker caches each COPY as its own layer.
# Dependencies layer changes rarely → almost always a cache hit.
# Application layer changes every commit → always re-copied, but it's tiny.
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Switch to non-root user before starting the app
USER shopsphere

# Document which port this service listens on.
# EXPOSE doesn't actually publish the port — that's done in docker-compose
# or Kubernetes Service. It's documentation for humans and tooling.
EXPOSE 8888

# Health check — Docker uses this to know if the container is healthy.
# Kubernetes has its own probes, but this helps with docker compose.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1

# Start the application using Spring Boot's layered launcher.
# JVM flags explained:
#   -XX:+UseContainerSupport    → JVM respects Docker memory limits
#                                 Without this, JVM sees HOST RAM, not container limit
#                                 and may allocate more heap than the container allows
#                                 causing OOMKilled by Kubernetes
#   -XX:MaxRAMPercentage=75.0   → Use max 75% of container memory for heap
#                                 Leaves room for non-heap (Metaspace, threads, etc.)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "org.springframework.boot.loader.launch.JarLauncher"]