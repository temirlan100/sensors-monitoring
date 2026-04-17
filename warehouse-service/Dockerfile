FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Layer 1: Gradle wrapper + build scripts (cached until build config changes)
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY warehouse-service/build.gradle.kts warehouse-service/build.gradle.kts
COPY central-monitoring-service/build.gradle.kts central-monitoring-service/build.gradle.kts
RUN mkdir -p warehouse-service/src/main/kotlin \
    && mkdir -p central-monitoring-service/src/main/kotlin

# Layer 2: Download dependencies (cached until build.gradle.kts changes)
RUN chmod +x gradlew && ./gradlew :warehouse-service:dependencies --no-daemon

# Layer 3: Copy source code and build (only this layer rebuilds on code changes)
COPY warehouse-service/src warehouse-service/src
RUN ./gradlew :warehouse-service:quarkusBuild --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/warehouse-service/build/quarkus-app /app
EXPOSE 3344/udp
EXPOSE 3355/udp
EXPOSE 3366/udp
EXPOSE 3377/udp
CMD ["java", "-jar", "quarkus-run.jar"]
