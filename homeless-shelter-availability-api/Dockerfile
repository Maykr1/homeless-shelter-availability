# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY homeless-shelter-availability-api/pom.xml homeless-shelter-availability-api/
COPY homeless-shelter-availability-web/pom.xml homeless-shelter-availability-web/
RUN mvn dependency:go-offline -B
COPY homeless-shelter-availability-api/src ./homeless-shelter-availability-api/src
RUN mvn package -pl homeless-shelter-availability-api -am -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE
WORKDIR /app
COPY --from=builder /app/homeless-shelter-availability-api/target/homeless-shelter-availability-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
