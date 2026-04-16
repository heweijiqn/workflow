# LLM Workflow Service

一个基于LangChain4j和LangGraph4j的工作流服务，用于执行复杂的LLM工作流。

## 项目结构

```
├── src/main/java/com/gemantic/              # 应用源代码
│   ├── workflow/                            # 工作流运行时逻辑
│   │   ├── controller/                      # REST控制器
│   │   ├── consumer/                        # 节点消费者
│   │   ├── repository/                      # 工作流存储库
│   │   └── support/                         # 支持类
│   └── resources/                           # Spring配置文件
├── src/test/java/                           # 测试代码
└── pom.xml                                  # Maven配置文件
```

## 技术栈

- Java 21
- Spring Boot
- LangChain4j
- LangGraph4j

## 构建和运行

### 前提条件

- JDK 21
- Maven 3.8+

### 构建项目

```bash
mvn clean package -DskipTests
```

### 运行项目

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 工作流执行流程

1. 接收工作流计划
2. 使用LangGraph4j构建工作流图
3. 执行工作流图
4. 返回执行结果

## 消费者

项目包含多种类型的消费者，用于执行不同类型的工作流节点：

- SqlQuery：执行SQL查询
- QAgent：使用Agent执行任务
- DocOutput：输出文档

## 配置

项目使用Spring Boot配置文件，支持不同环境的配置：

- application.yml：默认配置
- application-local.yml：本地开发环境配置
- application-dev.yml：开发环境配置
- application-prod.yml：生产环境配置

### 环境变量

项目需要配置以下环境变量：

| 环境变量 | 描述 | 默认值 |
|---------|------|--------|
| GENERAL_GPT_SERVICE_URL | 通用GPT服务URL | - |
| CONSUMER_BATCH_SIZE | 消费者批处理大小 | 10 |
| CONSUMER_CONCURRENT | 是否启用并发消费 | true |
| OKHTTP_CONNECT_TIMEOUT | OkHttp连接超时时间（秒） | 30 |
| OKHTTP_READ_TIMEOUT | OkHttp读取超时时间（秒） | 600 |
| OKHTTP_WRITE_TIMEOUT | OkHttp写入超时时间（秒） | 600 |
| OKHTTP_CALL_TIMEOUT | OkHttp调用超时时间（秒） | 1200 |

## 贡献

欢迎提交Issue和Pull Request！
