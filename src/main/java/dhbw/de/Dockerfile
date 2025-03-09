
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/WetterAPI_funk_gommlich_dhbw-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/WetterAPI_funk_gommlich_dhbw-1.0-SNAPSHOT.jar"]
