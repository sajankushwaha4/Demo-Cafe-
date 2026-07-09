# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml to download dependencies (helps with Docker caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the default port (will be overridden by PORT env variable in cloud)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
