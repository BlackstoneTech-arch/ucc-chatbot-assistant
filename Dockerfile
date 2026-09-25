FROM maven:3.9.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
COPY backend/src ./backend/src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/ucc-chatbot-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
