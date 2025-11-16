# Use official Java 17 runtime (your project uses Java 17)
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven wrapper & dependency files first (better caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download all Maven dependencies (cached layer)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy the actual source code
COPY src src

# Build the Spring Boot application
RUN ./mvnw package -DskipTests

# Expose port (same as server.port in application.yml)
EXPOSE 8080

# Run the produced jar
CMD ["java", "-jar", "target/billing-and-stock-management-app-0.0.1-SNAPSHOT.jar"]
