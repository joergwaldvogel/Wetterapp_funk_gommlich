# Backend Dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY ../../../../.. .
RUN ./gradlew build -x tes

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]