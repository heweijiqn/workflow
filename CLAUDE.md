# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**llm-workflow-service** is an LLM workflow orchestration engine built on Spring Boot 3.x with JDK 21. It provides a consumer-based architecture for executing workflow nodes asynchronously through Redis queues.

**Port**: 31001
**Base Package**: `com.gemantic`

## Essential Commands

### Build & Test
```bash
# Clean build with tests
mvn clean test

# Build without tests (for packaging)
mvn clean package -DskipTests

# Run specific test class
mvn test -Dtest=SqlQueryTest

# Run specific test method
mvn test -Dtest=SqlQueryTest#testQueryExecution
```

### Local Development
```bash
# Run with local profile (consumer disabled)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run with dev profile (consumer enabled)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Architecture

### Consumer-Based Workflow Engine

The core architecture follows a **message-driven workflow orchestration** pattern:

```
WorkflowRepository → Redis Queue → BaseConsumer → Node Execution → Update Status → Push Next Nodes
```

**Key Components**:
- `BaseConsumer`: Abstract base class for all node consumers
- `WorkflowRepository`: Workflow orchestration and execution planning
- `ConsumerRepository`: Node execution and result handling
- Redis Queue: Task distribution (QUEUE/ZSET/TABLE modes)

### Node Consumer Pattern

All workflow nodes extend `BaseConsumer` or its subclasses (`BasePrompt`, `BaseQChat`):

**Consumer Hierarchy**:
```
BaseConsumer (abstract)
├── BasePrompt (LLM prompt nodes)
│   ├── SimplePrompt
│   ├── ListPrompt
│   └── ...
├── BaseQChat (Q-series nodes)
│   ├── QDb (database query)
│   ├── QDoc (document query)
│   ├── QAgent (intelligent agent)
│   └── ...
└── Direct implementations
    ├── SqlQuery
    ├── ApiRequest
    ├── DocOutput
    └── ...
```

**Adding a New Node Type**:
1. Create class in `com.gemantic.workflow.consumer`
2. Extend `BaseConsumer` (or `BasePrompt`/`BaseQChat`)
3. Add `@Component("NodeTypeName")` annotation
4. Implement `execute()` method
5. Override `available()` if conditional enablement needed

### Configuration Profiles

- **local**: Development with consumer disabled, Swagger enabled, DEBUG logging
- **dev**: Development environment, consumer enabled, INFO logging
- **prod**: Production environment, Swagger disabled, INFO logging
- **high-concurrency**: Optimized for high-load scenarios

Profile files: `src/main/resources/application-{profile}.yml`

## Database Optimization Patterns

**Critical**: This codebase has been heavily optimized to eliminate N+1 queries and reduce database calls by 70-95%. When modifying database operations:

### Batch Query Pattern
```java
// WRONG: N+1 query problem
for (Long nodeId : nodeIds) {
    WorkflowRunResult node = client.getById(nodeId);
    // process node
}

// CORRECT: Batch query
List<WorkflowRunResult> nodes = batchGetNodesByIds(nodeIds);
Map<Long, WorkflowRunResult> nodeMap = nodes.stream()
    .collect(Collectors.toMap(WorkflowRunResult::getId, n -> n));
```

### Avoid Duplicate Updates
```java
// WRONG: Multiple updates to same entity
client.updateStatus(nodeId, "RUNNING");
// ... some logic ...
client.updateStatus(nodeId, "RUNNING"); // duplicate!

// CORRECT: Single update
client.updateStatus(nodeId, "RUNNING");
```

**Reference**: See `optimization/` directory for detailed optimization documentation.

## Testing Guidelines

### Test Structure
- Test classes: `src/test/java/com/gemantic/`
- Test fixtures: `src/test/resources/` (JSON payloads)
- Framework: JUnit (mix of JUnit 4 and JUnit Jupiter)

### Test Naming
- Test classes: `*Test.java` (e.g., `SqlQueryTest.java`)
- Test methods: `test*` or `@Test` annotated

### Coverage Requirements
- Cover happy path and failure scenarios
- Test consumer logic with realistic JSON fixtures
- Verify node state transitions (QUEUED → RUNNING → FINISHED/FAILED)

## Key Dependencies

- **Spring Boot**: 3.x (parent: spring-boot-gemantic-service-parent 3.5.6)
- **Databases**: MySQL, PostgreSQL, KingBase8, DuckDB (1.1.1)
- **JSON**: Fastjson2 (2.0.55)
- **SQL Parsing**: JSQLParser (4.9)
- **Template Engine**: FreeMarker (2.3.31)
- **Markdown**: Flexmark (0.64.8)
- **Connection Pool**: Alibaba Druid

## Consumer Configuration

Key application properties:
```yaml
consumer.enable: true/false          # Enable/disable consumers
consumer.mode: QUEUE/ZSET/TABLE      # Queue implementation mode
consumer.intervalMilliseconds: 2000  # Polling interval
node.run.timeout.milliseconds: 1800000  # Node timeout (30 min)
scheduled.thread.pool: 200           # Thread pool size
```

## Workflow Execution Flow

1. **Initialization**: `WorkflowRepository.runWorkflow()` creates workflow run
2. **Planning**: Generate execution plan with node dependencies
3. **Queue Push**: Push starting nodes to Redis queue
4. **Consumer Pull**: `BaseConsumer.run()` pulls tasks from queue
5. **Execution**: Node-specific `execute()` logic runs
6. **Status Update**: Update node status (FINISHED/FAILED)
7. **Propagation**: Push downstream nodes to queue
8. **Completion**: All nodes finish, workflow status updated

## Timeout & Retry Mechanisms

- **RunTimeoutJob**: Detects nodes running > 30 minutes (configurable)
- **QueueTimeoutJob**: Re-queues nodes stuck in QUEUED state > 2 minutes
- **SubWorkflowStatusJob**: Repairs sub-workflow status inconsistencies

## Common Patterns

### Feign Client Usage
External services are accessed via Feign clients in `com.gemantic.workflow.client`:
- `WorkflowClient`, `WorkflowRunClient`, `WorkflowRunResultClient`
- `OpenQdataDbClient`, `OpenQdataDocClient`, `OpenQagentClient`
- Always handle Feign exceptions and provide fallback logic

### JSON Processing
Use Fastjson2 for all JSON operations:
```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

JSONObject obj = JSON.parseObject(jsonString);
String json = JSON.toJSONString(object);
```

### Logging
```java
private static final Logger LOG = LoggerFactory.getLogger(ClassName.class);

LOG.debug("Detailed debug info");
LOG.info("Key business flow");
LOG.warn("Warning with context");
LOG.error("Error message", exception);
```

## Documentation References

- **Architecture Details**: `openspec/project.md` (comprehensive Chinese documentation)
- **Optimization Guide**: `optimization/README.md` (database optimization patterns)
- **Node Configurations**: `docs/*.json` (node type configurations)
- **Deployment**: `k8s/README.md` (Kubernetes deployment)

## Important Notes

- **JDK Version**: Must use JDK 21 for development
- **Immutability**: Follow immutable patterns where possible (see global rules)
- **Security**: Never commit database credentials or API keys
- **Thread Safety**: Consumer methods may be called concurrently
- **Transaction Boundaries**: Be careful with `@Transactional` in consumer logic
