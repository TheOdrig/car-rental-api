FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/car-rental-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082

# JVM options configurable via environment variable
# Default: container-aware with 75% RAM usage
# Override: docker run -e JAVA_OPTS="-Xmx1g -Xms512m" ...
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

