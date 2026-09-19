# JayCong Boot Framework

基于 Spring Boot 的模块化 Java 基础框架，用于日常开发和学习。

## 当前阶段

已初始化 Maven 多模块工程、版本管理及依赖关系，并实现 Core 的统一返回、
错误码、业务异常和分页模型。Web Starter 已实现自动配置、全局异常处理和
MVC 参数校验；Example 提供可运行的最小接口与真实 HTTP 集成测试。
MyBatis Starter 已实现自动分页、基础实体和时间审计填充；Example 提供 H2 CRUD、
分页接口及 MySQL profile。Example 已启用 Spring Boot repackage，可直接运行打包 JAR。

## 环境

- JDK 21
- Maven 3.9+
- Spring Boot 3.5.16
- MyBatis-Plus 3.5.17

版本选取参考 [Spring 官方发布信息](https://spring.io/blog/category/releases)、
[MyBatis-Plus 官方文档](https://baomidou.com/getting-started/install/)，并核对 Maven Central 已发布制品。

## 模块

| 模块 | 职责 |
| --- | --- |
| jaycong-bom | 独立 BOM，管理框架、Spring Boot 和 MyBatis-Plus 依赖版本 |
| jaycong-core | 已实现基础模型、错误码与异常；零运行时依赖，JUnit 仅用于测试 |
| jaycong-context | 请求/用户上下文及 Spring 线程池扩展 |
| jaycong-web-spring-boot-starter | Servlet 自动配置、统一异常响应、MVC 参数校验 |
| jaycong-mybatis-spring-boot-starter | MyBatis-Plus 自动配置、分页转换、基础实体和时间审计 |
| jaycong-example | 可运行的 Web/CRUD 示例、H2 默认配置及 MySQL profile |
| jaycong-order-service | A 服务：创建订单，通过 HTTP 调用 B，提供本地事务回滚演示 |
| jaycong-stock-service | B 服务：独立库存数据库、原子扣减接口 |
| docs | 设计和开发文档，不是 Maven 模块 |

依赖方向：Example → Web/MyBatis Starter → Context → Core。
根 POM 聚合全部模块并管理构建；BOM 不继承根 POM，避免循环导入。
数据库驱动仅在 Example 中引入，使用框架的业务项目自行选择驱动。

## 构建

在项目根目录运行：

```powershell
mvn -B -ntp clean verify
```

安装到本地仓库，供其他项目使用：

```powershell
mvn -B -ntp clean install
```

仅构建一个模块及其所需模块：

```powershell
mvn -B -ntp -pl jaycong-web-spring-boot-starter -am verify
```

IDEA 中打开根目录的 pom.xml，项目 SDK 选择 JDK 21，加载 Maven 工程。
构建会执行 Core 契约测试、Web Starter 测试、MyBatis 自动配置及 H2 测试，
以及 Example 的真实 HTTP 集成测试。

运行最小示例（默认端口 8080，使用自动配置的 H2 内存数据库，无需外部数据库）：

```powershell
java -jar jaycong-example/target/jaycong-example-0.1.0-SNAPSHOT.jar
```

接口调用、异常映射和自动配置扩展见 [Web Starter 使用说明](docs/web.md)。
数据库接入、分页、审计字段及 CRUD 接口见 [MyBatis Starter 使用说明](docs/mybatis.md)。

A 服务调用 B 服务的下单扣库存示例见 [订单与库存服务使用说明](docs/order-stock.md)。
默认分别运行在 8081、8082，使用独立 H2 数据库，并提供 MySQL 配置。
当前仅使用本地事务，可通过故障开关演示跨服务数据不一致，尚未引入分布式事务。

## Core 使用

使用示例及错误码、分页约定见 [Core 使用说明](docs/core.md)。

## 其他项目如何引入

先在本项目运行 install，然后在业务项目中导入 BOM 并声明所需 Starter：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.jaycong</groupId>
            <artifactId>jaycong-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.jaycong</groupId>
        <artifactId>jaycong-web-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

BOM 只管理依赖版本，不引入功能，也不管理消费项目的构建插件。
业务项目需自行配置 Java 21 编译与 Spring Boot 应用打包。

## 后续开发顺序

1. Core（已完成）：统一返回对象、错误码、业务异常和分页模型。
2. Context：上下文保存、清理及线程池透传。
3. Web：自动配置、异常映射、MVC 参数校验已完成；TraceId 待实现。
4. MyBatis：基础实体、分页、时间审计字段自动填充已完成；用户审计待 Context 接入。
5. Example：Web/CRUD 示例与 HTTP 集成测试、H2 和 MySQL 配置已完成。
