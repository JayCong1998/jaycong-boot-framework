# 会员操作流水实施计划

**Goal:** 为现有会员变更提供可追溯流水及后台只读查询。

**Architecture:** User 应用层编排审计与事务，domain 定义流水模型及仓储，infrastructure 使用 MyBatis-Plus 持久化。管理端复用当前 HTTP 客户端及管理员路由守卫。

**Tech Stack:** Java 21、Spring Boot、TransactionTemplate、MyBatis-Plus、Vue 3、Element Plus。

- [x] 新增流水模型、查询模型和仓储接口；提供继承 BaseEntity 的数据库实体、Mapper、仓储实现及手工建表 SQL。
- [x] 应用服务持久化操作开始与阶段结果；现有 MemberService 接入昵称、状态和会话撤销，避免日志错误伪装业务成功。
- [x] 新增 /admin/members/operations 分页和详情，只允许管理员，校验分页、枚举、ID 和时间范围。
- [x] 新增前端查询页及详情，导航形成会员管理下会员列表/操作记录，保留原 URL 和安全回跳。
- [x] 更新使用文档，执行 User Maven package -DskipTests 及 mall-admin npm run build，检查改动与局限；不新增或修改测试。
