# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml ./
COPY homeless-shelter-availability-api/pom.xml homeless-shelter-availability-api/pom.xml
COPY homeless-shelter-availability-web/pom.xml homeless-shelter-availability-web/pom.xml
RUN mvn -pl homeless-shelter-availability-api -am dependency:go-offline -B
COPY homeless-shelter-availability-api/src homeless-shelter-availability-api/src
RUN mvn -pl homeless-shelter-availability-api -am package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/homeless-shelter-availability-api/target/homeless-shelter-availability-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
