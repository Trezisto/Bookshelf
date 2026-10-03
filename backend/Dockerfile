# Build stage: official Gradle image with a JDK
FROM gradle:jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# SQLite database file lives on a volume
ENV LIBRARY_DB_PATH=/data/library.db
VOLUME /data
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
