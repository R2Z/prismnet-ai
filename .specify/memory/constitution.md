<!--
Version change: 0.0.0 → 1.0.0
List of modified principles: None (new constitution)
Added sections: Core Principles, Technology Stack Requirements, Development Workflow, Governance
Removed sections: None
Templates requiring updates: None - templates are generic and don't reference specific principles yet
Follow-up TODOs: None
-->

# PrismNet AI Constitution

## Core Principles

### I. Clean Architecture & SOLID Principles
All code MUST follow Clean Architecture principles with proper separation of concerns. Each class and method MUST have a single responsibility. Use dependency inversion for external dependencies (inject dependencies rather than creating them directly). Apply interface segregation where appropriate. Make classes open for extension but closed for modification.

### II. Modern Java Best Practices
Use Streams API for collection processing where it improves readability. Leverage Optional to handle null values safely. Prefer immutable objects and collections where possible. Use efficient collection types based on usage patterns. Apply var keyword judiciously for local variables when type is obvious.

### III. Test-First Development (NON-NEGOTIABLE)
TDD mandatory: Tests written → User approved → Tests fail → Then implement. Red-Green-Refactor cycle strictly enforced. Use JUnit 5 and Mockito for unit testing. Testcontainers for integration testing.

### IV. Observability & Monitoring
Implement comprehensive logging with @Slf4j annotation. Log at INFO level for method entry/exit, DEBUG for decisions and branches, WARN for recoverable issues, ERROR for failures. Include contextual information like method names and identifiers. Avoid logging sensitive information. Use Micrometer with Prometheus for metrics and OpenTelemetry for distributed tracing.

### V. Security-First Approach
OAuth2/JWT security implementation required. All endpoints MUST be secured. Input validation and sanitization mandatory. Use Spring Security best practices. Regular security audits and dependency updates required.

## Technology Stack Requirements

### VI. Spring Boot 3.x Standards
All projects MUST use Spring Boot 3.x with Java 17. Maven build system with proper dependency management. Spring Data JPA for data persistence. Flyway for database migrations. Spring Boot Actuator for monitoring and management.

### VII. Database & Persistence
MySQL database configuration required. Proper entity relationships and constraints. Database migrations versioned and documented. Connection pooling and performance optimization.

### VIII. Production Readiness
All code paths MUST be testable. Code easily extensible for future requirements. Appropriate validation for method parameters. Performance implications considered. Thread-safe where concurrent access possible.

## Development Workflow

### IX. Code Quality Standards
Comprehensive JavaDoc for all public methods and classes with @param, @return, and @throws tags. Methods under 20 lines preferred. Guard clauses and early returns for complex logic. Descriptive method and variable names.

### X. Error Handling & Resilience
Replace generic exceptions with specific, meaningful exception types. Create custom exceptions for domain-specific errors. Provide clear, actionable error messages. Proper exception chaining to preserve stack traces. Handle exceptions at appropriate levels.

## Governance

Constitution supersedes all other practices. All PRs/reviews MUST verify compliance with these principles. Complexity MUST be justified. Amendments require documentation, approval, migration plan, and version increment according to semantic versioning.

**Version**: 1.0.0 | **Ratified**: 2025-11-09 | **Last Amended**: 2025-11-09
