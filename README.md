# Agentic URL Shortener

## Overview

This project is a URL Shortener application built using Java and Spring Boot. Along with the URL shortening functionality, I implemented an agentic workflow that demonstrates how an engineering task can be planned, executed, validated, and completed through different workflow stages.

The goal of this project was to build a working application while showcasing concepts such as workflow orchestration, dependency management, retries, rollback, human approval, policy validation, audit logging, and dynamic replanning.


## Features

### URL Shortener

- Create short URLs
- Redirect users to the original URL
- Track click analytics
- Support URL expiration
- Validate user input
- Global exception handling
- H2 database integration

### Agentic Workflow

- Requirement Analysis
- Task Decomposition
- Implementation
- Parallel Testing and Security Review
- Human Approval
- Documentation
- Retry mechanism
- Rollback
- Dynamic Replanning
- Policy Guardrails
- Safe Stop
- Audit Trail
- Workflow Metrics


## Workflow

The workflow follows this sequence:

Requirement Analysis
↓
Task Decomposition
↓
Implementation
↓
Testing + Security Review (Parallel)
↓
Human Approval
↓
Documentation
↓
Completed

Testing and Security Review execute in parallel after Implementation. The workflow waits until both tasks finish before moving to the approval stage.



## Technologies Used

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Web
- H2 Database
- Maven
- JUnit 5
- Mockito
- CompletableFuture


## Architecture

The system architecture is documented here:

📄 [Architecture Documentation](docs/architecture.md)

## Project Structure

```
src
 ├── api
 ├── domain
 ├── dto
 ├── repository
 ├── service
 ├── orchestration
 │     ├── approval
 │     ├── audit
 │     ├── engine
 │     ├── graph
 │     ├── metrics
 │     ├── policy
 │     └── state
 └── config
```



## Running the Project

Clone the repository

```bash
git clone <repository-url>
```

Build the project

```bash
mvn clean install
```

Run the application

```bash
mvn spring-boot:run
```

Run the tests

```bash
mvn test
```



## Workflow Capabilities

The orchestration engine demonstrates:

- Sequential task execution
- Parallel task execution
- Dependency graph
- Retry handling
- Rollback on failures
- Human approval before controlled actions
- Policy validation
- Audit logging
- Dynamic replanning
- Safe stop for policy violations


## Engineering Scenarios

The project demonstrates three software engineering scenarios required by the assignment:

- 🌱 Greenfield Development
- 🔄 Brownfield Enhancement
- ❓ Ambiguous Requirement Handling

For detailed workflows, see:

- 📄 [Engineering Scenarios](docs/scenarios.md)
## Engineering Summary

For implementation details and engineering decisions, see:

- 📄 [Engineering Summary](docs/engineering-summary.md)

## Future Improvements

Some enhancements that could be added in the future include:

- AI-powered requirement analysis
- Integration with GitHub
- Docker support
- CI/CD pipeline
- Persistent workflow storage
- Role-based approvals
- OpenAPI documentation
- PostgreSQL support



## Assignment Summary

This project demonstrates both a working URL Shortener application and an agentic workflow engine. The focus was on showing how software engineering activities can be automated while maintaining reliability through retries, policy validation, approvals, audit logging, rollback, and dynamic replanning.

The project was developed incrementally, with each feature implemented, tested, and validated before moving to the next stage.
