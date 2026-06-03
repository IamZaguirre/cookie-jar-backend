# Build stage (unchanged)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests clean package
# ✅ Switch to Alpine-based JRE (much smaller)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/cookie-jar-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# ✅ Add JVM memory limits to reduce RAM usage
ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-jar", "/app/app.jar"]