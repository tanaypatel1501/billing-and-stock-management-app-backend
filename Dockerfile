# --------------------------------------------------------------------
# STAGE 1: BUILDER
# Use official Java 17 JDK to build the project
# --------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 1. Copy build files first (better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 2. Download dependencies (Cached unless pom.xml changes)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# 3. Copy source code
COPY src src

# 4. Build the application
RUN ./mvnw clean package -DskipTests

# 5. Extract the Layers from the JAR
# We use the exact finalName defined in your pom.xml
RUN java -Djarmode=layertools -jar target/billing-and-stock-management-app.jar extract --destination extracted

# --------------------------------------------------------------------
# STAGE 2: RUNTIME
# Use a smaller JRE image for production
# --------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the extracted layers from the builder stage
# Dependencies change least often, so they go first (better caching)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Expose port (Render provides PORT env var, but 8080 is a good default)
EXPOSE 8080

# Use JarLauncher to start the app (Optimized for layers)
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]