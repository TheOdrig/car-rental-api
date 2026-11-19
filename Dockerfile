FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /layers
COPY --from=build /workspace/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
ENV TZ=UTC

RUN addgroup -S -g 1001 spring && adduser -S -u 1001 -G spring spring
COPY --from=layers /layers/extracted/dependencies/ ./
COPY --from=layers /layers/extracted/spring-boot-loader/ ./
COPY --from=layers /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers /layers/extracted/application/ ./

USER spring
EXPOSE 8082 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${MANAGEMENT_PORT:-8081}/actuator/health/readiness" || exit 1
ENTRYPOINT ["sh", "-c", "exec java -Duser.timezone=UTC $JAVA_OPTS -jar application.jar"]
