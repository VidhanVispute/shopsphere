FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/inventory-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]