# Auth 数据库初始化实施计划

**目标：** 按用户确认的本机 MySQL 配置连接 jay-cong-framework，提供用户表及开发账号手工 SQL。

**架构：** Auth 依赖 common-mybatis 和运行时 MySQL 驱动；沿用 Spring Boot 数据源和数据库健康检查。SQL 不自动执行，不实现用户写入、登录和 Token。

**技术栈：** Java 21、Spring Boot 3.5、MyBatis-Plus、MySQL 8。

- [x] 修改 jaycong-auth/pom.xml 添加受 BOM 管理的数据库依赖。
- [x] 修改 application.yml，默认连接 localhost:3306/jay-cong-framework，支持 AUTH_DB_URL、AUTH_DB_USERNAME、AUTH_DB_PASSWORD 覆盖，禁用 SQL 自动初始化。
- [x] 添加 db/schema-mysql.sql 和 db/data-mysql.sql，遵守 AGENTS.md 的字段、索引和开发账号约定。
- [x] 更新 docs/auth.md，说明连接、手工初始化和当前业务边界。
- [x] 执行 mvn -B -ntp -pl jaycong-auth -am package -DskipTests。
- [ ] 启动生成的 JAR，验证 /auth/status 和 /actuator/health；仅验证进程临时展示 db 健康详情，完成后停止。

不新增、编写或修改测试代码，不执行建表或账号 SQL。

## 验证结果

Maven package -DskipTests 成功。临时端口 18084 启动成功，/auth/status 返回 code=0；/actuator/health 返回 HTTP 503，db=DOWN。根因：MySQL Access denied for user 'root'@'localhost' (using password: YES)。等待提供有效凭证后继续验证数据库；未执行任何 SQL。
