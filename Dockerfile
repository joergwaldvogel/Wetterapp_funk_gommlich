# 1. Stufe: Frontend Build
FROM node:18 AS frontend-build
WORKDIR /app
COPY frontend/ .
RUN npm install && npm run build

# 2. Stufe: Backend Build
FROM eclipse-temurin:17 AS backend-build
WORKDIR /app
COPY backend/ .
RUN ./gradlew build -x test

# 3. Stufe: Endgültiges Image
FROM eclipse-temurin:17
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
COPY --from=frontend-build /app/dist/ src/main/resources/static/
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]