# JayCong Boot Framework

基于 Spring Boot 的模块化 Java 基础框架，用于日常开发和学习。

## 当前阶段

已初始化 Maven 多模块工程、版本管理及依赖关系，并实现 Core 的统一返回、
错误码、业务异常和分页模型。Web Starter 已实现自动配置、全局异常处理和
MVC 参数校验。Datasource 模块提供 MyBatis-Plus 分页、逻辑删除、乐观锁和审计字段填充。
当前可运行应用包含 Auth、Admin、User、Goods、Order 与 Gateway。后台管理请求统一通过 Admin 调用业务服务。

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
| jaycong-admin | 后台管理统一入口，通过 RPC 调用 User/Goods，与 Auth、Gateway 同级，默认端口 8088，见 [使用说明](docs/admin.md) |
| jaycong-auth | 认证 HTTP 入口，远程调用 User 并管理 Redis 会话，默认端口 8084，见 [使用说明](docs/auth.md) |
| jaycong-user | 用户领域与 Facade 服务，位于 Business 下，持有数据库和会员管理能力，默认端口 8087，见 [使用说明](docs/user.md) |
| jaycong-business | 业务模块聚合父模块，子模块继承其 POM |
| jaycong-goods | 商品业务模块骨架，位于 `jaycong-business/` 下 |
| jaycong-pay | 支付业务模块骨架，位于 `jaycong-business/` 下 |
| jaycong-order | 订单业务模块骨架，位于 `jaycong-business/` 下 |
| jaycong-gateway | WebFlux 微服务网关，默认端口 8090，见 [使用说明](docs/gateway.md) |
| jaycong-bom | 独立 BOM，管理框架、Spring Boot 和 MyBatis-Plus 依赖版本 |
| jaycong-common | 公共技术模块聚合父模块，不作为业务功能依赖 |
| jaycong-common-rpc | OpenFeign 与 LoadBalancer 依赖组合，goods、order、pay 已引用，见 [接入说明](docs/rpc.md) |
| jaycong-common-core | 已实现基础模型、错误码与异常；零运行时依赖，JUnit 仅用于测试 |
| jaycong-common-web | Servlet 自动配置、统一异常响应、MVC 参数校验 |
| jaycong-common-datasource | MyBatis-Plus 自动配置、分页、逻辑删除、乐观锁和审计字段填充 |
| jaycong-common-satoken | Sa-Token、Redis 会话与 AuthContext 登录身份入口，见 [接入说明](docs/satoken.md) |
| jaycong-common-redis | Redis Starter 与默认字符串键、JSON 值序列化配置 |
| jaycong-common-config | Nacos 注册发现与配置中心，见 [使用说明](docs/config.md) |
| docs | 设计和开发文档，不是 Maven 模块 |
| mall-admin | 独立 Vue 3 + Element Plus 运营后台，默认端口 5174，含 Auth 登录/退出，见 [启动说明](mall-admin/README.md)；不是 Maven 模块 |

依赖方向：业务应用 → Web/Datasource → Core。
根 POM 聚合全部模块并管理构建；六个公共模块位于 `jaycong-common/` 下，继承 Common 聚合父 POM。
Common 不统一引入功能依赖，业务按需引入具体子模块；BOM 不继承根 POM，避免循环导入。
本次迁移统一了 Maven artifactId，Java 包名保持不变；外部项目需更新依赖坐标并重新 install。
模块路径、坐标及依赖说明见 [Common 模块说明](docs/common.md)。
Common Datasource 默认包含 MySQL JDBC runtime 驱动；其他数据库项目可排除该驱动并添加所需驱动。

Business 聚合 goods、pay、order、user 业务模块；User 承载用户领域、Facade 和会员管理，Auth 通过 OpenFeign 调用 User。各模块按需引入 Common 子模块，应用间共享 common-api 契约，不依赖对方应用 JAR。
这些业务模块暂不面向外部项目提供公共依赖，因此未加入 BOM。
新增业务模块时，在 `jaycong-business/pom.xml` 的 `<modules>` 中注册，并继承该父 POM。

## 构建

在项目根目录运行：

```powershell
mvn -B -ntp package -DskipTests
```

安装到本地仓库，供其他项目使用：

```powershell
mvn -B -ntp install -DskipTests
```

仅构建一个模块及其所需模块：

```powershell
mvn -B -ntp -pl :jaycong-common-web -am package -DskipTests
```

IDEA 中打开根目录的 pom.xml，项目 SDK 选择 JDK 21，加载 Maven 工程。
Auth 服务启动方式见 [Auth 使用说明](docs/auth.md)，网关启动与路由见 [Gateway 使用说明](docs/gateway.md)。

接口异常映射和自动配置扩展见 [Web Starter 使用说明](docs/web.md)。
数据库接入、分页、逻辑删除、乐观锁及审计字段见 [Datasource 模块说明](docs/datasource.md)。

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
        <artifactId>jaycong-common-web</artifactId>
    </dependency>
</dependencies>
```

BOM 只管理依赖版本，不引入功能，也不管理消费项目的构建插件。
业务项目需自行配置 Java 21 编译与 Spring Boot 应用打包。

## 后续开发顺序

1. Core（已完成）：统一返回对象、错误码、业务异常和分页模型。
2. 上下文：暂不保留空模块，待认证与异步透传需求明确后设计。
3. Web：自动配置、异常映射、MVC 参数校验已完成；TraceId 待实现。
4. Datasource：基础实体、分页、逻辑删除、乐观锁和审计字段自动填充已完成。
