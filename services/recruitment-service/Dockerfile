# Use openjdk base image and install Maven
FROM openjdk:17-jdk-slim AS build

# Install curl, wget and Maven
RUN apt-get update && apt-get install -y curl wget && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz | tar -xzf - -C /opt && \
    ln -s /opt/apache-maven-3.8.6/bin/mvn /usr/local/bin/mvn

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Install wget and curl for health checks and startup script
RUN apt-get update && apt-get install -y wget curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built jar file
COPY --from=build /app/target/*.jar app.jar

# Create non-root user for security
RUN adduser --disabled-password --gecos "" appuser && chown -R appuser /app
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]


