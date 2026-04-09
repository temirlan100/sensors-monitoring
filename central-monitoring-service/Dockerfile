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
RUN chmod +x gradlew && ./gradlew :central-monitoring-service:dependencies --no-daemon

# Layer 3: Copy source code and build (only this layer rebuilds on code changes)
COPY central-monitoring-service/src central-monitoring-service/src
RUN ./gradlew :central-monitoring-service:quarkusBuild --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/central-monitoring-service/build/quarkus-app /app
CMD ["java", "-jar", "quarkus-run.jar"]
