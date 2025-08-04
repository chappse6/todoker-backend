# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Todoker Backend is a Spring Boot 3.2.2 application written in Kotlin that provides a REST API for a todo/task management system. The project uses modern Spring Boot architecture with JPA, security, and monitoring capabilities.

## Technology Stack

- **Framework**: Spring Boot 3.2.2 with Spring Security, Spring Data JPA
- **Language**: Kotlin 1.9.22 on Java 17
- **Database**: MariaDB with Flyway migrations
- **Cache**: Redis
- **Query**: QueryDSL for type-safe queries
- **Authentication**: JWT (JSON Web Tokens)
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, Testcontainers for integration tests
- **Monitoring**: Micrometer with Prometheus metrics

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with verbose output
./gradlew test --info

# Run specific test class
./gradlew test --tests "com.todoker.todokerbackend.TodokerBackendApplicationTests"

# Run tests in continuous mode
./gradlew test --continuous
```

### Code Quality
```bash
# Check Kotlin code style (if ktlint is added)
./gradlew ktlintCheck

# Format Kotlin code (if ktlint is added)
./gradlew ktlintFormat
```

## Architecture

### Package Structure
- `com.todoker.todokerbackend` - Root package containing the main application class
- The project follows standard Spring Boot conventions with separation between:
  - Main application code (`src/main/kotlin`)
  - Resources and configuration (`src/main/resources`)
  - Test code (`src/test/kotlin`)

### Key Dependencies and Their Purposes
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **QueryDSL** - Type-safe query generation (configured with kapt annotation processor)
- **Flyway** - Database schema migrations
- **JWT libraries** - Token-based authentication
- **Testcontainers** - Integration testing with real database instances
- **Spring Boot Actuator** - Health checks and monitoring endpoints

### QueryDSL Configuration
The project is configured to use QueryDSL with Kotlin annotation processing (kapt). Generated Q-classes will be created in the build directory for type-safe database queries.

### Database Setup
- Primary database: MariaDB
- Caching layer: Redis
- Migration tool: Flyway with MySQL dialect

## Development Notes

### Spring Boot Configuration
- Application properties are minimal by default (`application.properties`)
- Consider using profiles for different environments (dev, test, prod)

### Testing Strategy
- Uses Testcontainers for integration tests with real MariaDB instances
- Spring Security test utilities available for authentication testing
- JUnit 5 platform with Spring Boot test support

### Monitoring and Observability
- Prometheus metrics endpoint available via Micrometer
- Spring Boot Actuator provides health checks and application info

### Security Considerations
- JWT-based authentication is configured
- Spring Security is integrated for comprehensive security features
- Remember to configure proper CORS settings for frontend integration