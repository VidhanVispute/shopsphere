FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk upgrade --no-cache
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]