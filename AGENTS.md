# Repository Guidelines

## Scope
This guide is for contributors working on the Java service code in this repository. It does not cover Kubernetes manifests or release packaging workflows.

## Project Structure & Module Organization
- `src/main/java/com/gemantic/`: application source code.
- `src/main/java/com/gemantic/workflow/`: workflow runtime logic.
- `src/main/java/com/gemantic/workflow/controller/`: REST controllers.
- `src/main/java/com/gemantic/workflow/consumer/`: node consumers (e.g., `SqlQuery`, `QAgent`, `DocOutput`).
- `src/main/resources/`: Spring profiles and logging config (`application-*.yml`, `logback-spring.xml`).
- `src/test/java/`: unit and integration-style tests.
- `src/test/resources/`: JSON fixtures and test payloads.

## Build, Test, and Development Commands
- `mvn clean test`: compile and run the test suite.
- `mvn test -Dtest=SqlQueryTest`: run one test class.
- `mvn clean package -DskipTests`: build artifact quickly for packaging.
- `mvn spring-boot:run -Dspring-boot.run.profiles=local`: run locally with local profile.

Use JDK 21 for local development to match the repository baseline.

## Coding Style & Naming Conventions
- Follow standard Java style: 4-space indentation, UTF-8 source files, one top-level class per file.
- Naming conventions: classes use `PascalCase` (e.g., `WorkflowRepositoryImpl`), methods/fields use `camelCase` (e.g., `runWorkflow`), and constants use `UPPER_SNAKE_CASE`.
- Keep package names lowercase under `com.gemantic`.
- Prefer focused classes by layer (`controller`, `repository`, `consumer`, `support`) and avoid cross-layer shortcuts.

## Testing Guidelines
- Primary frameworks are JUnit-based (`org.junit` with some JUnit Jupiter usage).
- Test class naming: `*Test.java`.
- Keep fixtures in `src/test/resources` and load realistic JSON inputs for workflow scenarios.
- Cover happy path and failure/edge behavior for any changed consumer or repository logic.

## Commit & Pull Request Guidelines
- Recent history favors short, direct commit titles (English or Chinese), e.g., `fix: remove extra logging`.
- Use small commits scoped to one behavior change.
- PRs should include what changed and why, affected modules/classes, test evidence (commands run and key results), and any config/profile impact (`application-*.yml`).
