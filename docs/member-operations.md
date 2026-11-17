# 会员操作记录

管理后台的“会员管理”下包含“会员列表”和“操作记录”。会员详情可跳转到该会员的流水，操作记录支持会员 ID、操作人 ID、操作类型、身份、结果和时间范围筛选，以及详情和刷新。页面和接口只读，不提供修改、删除。

## 部署

先手工执行 `jaycong-business/jaycong-user/src/main/resources/db/member-operation-mysql.sql`，再发布 User、Admin 和 mall-admin。本地开发库在本次联调已执行建表。其他环境仍需执行；应用不会自动建表。已有 `/admin/members/**` Gateway、Vite 和 Nginx 转发规则覆盖新接口，无需新增路径。

新增表为 `member_operation_log`，继承应用 ID、审计字段、逻辑删除和乐观锁配置，不使用外键。应用没有日志删除入口；历史留存和归档由后续明确的运维策略处理，不自动清理。

## 记录范围

当前接入管理员修改昵称、启用会员、禁用会员、重复禁用时重试撤销会话。操作人 ID、昵称快照及身份来自后端会话，前端不能指定。动作分别是 `UPDATE_NICKNAME`、`ENABLE`、`DISABLE`、`REVOKE_SESSIONS`。记录数据库提交的变更前后值，失败且数据库回滚时不保存未生效的变更值。

模型区分 `ADMIN` / `MEMBER` 身份与 `ADMIN_WEB` / `MEMBER_WEB` 来源。但当前项目尚无会员本人修改资料、密码和换绑接口，不能宣称这些场景已接入；后续新增入口时应从有效会话取操作人并复用审计编排，身份和来源不能由调用方请求提供。注册、登录、退出及普通查询不在本期流水范围内。

日志仅保存允许的昵称、状态及安全摘要，不保存密码、密码哈希、验证码、Token、请求原文、手机号/邮箱或底层异常。未授权请求、请求体校验未通过、状态接口目标预检查失败在执行前被拒绝，不产生业务流水；它不是覆盖所有 HTTP 请求的访问日志。

## 结果与一致性

| outcome | 含义 |
| --- | --- |
| PROCESSING | 已接收；仍在处理，或进程中断后结果需要核实 |
| SUCCESS | 操作的数据库阶段及所需的 Redis 阶段完成 |
| FAILED | 数据库变更未提交；业务冲突或外部处理失败 |
| PARTIAL | 数据库变更已提交，但会话撤销或最终结果写入未完成 |

开始流水先独立提交，写入失败不执行变更。数据库变更与阶段流水同事务：昵称修改一同提交成功，禁用先一同提交 PARTIAL，再在 Redis 撤销会话后标为 SUCCESS。启用前先清理 Redis 会话，清理失败不启用；即使之后版本冲突，旧会话可能已经撤销。DB/Redis 没有分布式事务，PROCESSING/PARTIAL 不应被解释为“完全未执行”，应刷新资料及按需重试撤销。重复操作会新增一条流水，不覆盖原失败记录。

## 接口

均要求正常管理员的 `Authorization: Bearer <tokenValue>`，普通会员返回 403，匿名返回 401。ID 一律以字符串输出，昵称使用操作时快照。

- `GET /admin/members/operations`：`page` 默认 1、`size` 默认 20（最大 100）；可选 `memberId`、`operatorId`、`action`、`operatorType`、`outcome`、`start`、`end`。
- `GET /admin/members/operations/{id}`：按流水 ID 查询详情，不存在返回 404。

列表返回 `Result<{ total, rows }>`，按创建时间、ID 倒序，时间格式 `2026-11-13T00:00:00`，按服务端本地时间解释，包含两端。非法分页、ID、枚举或反向时间范围返回 400。无历史流水回填。

## 实际验证（2026-11-13）

- `mvn -B -ntp -pl jaycong-business/jaycong-user -am package -DskipTests '-Dspring-boot.repackage.skip=true'`：通过。
- mall-admin `npm run build`：包含 TypeScript 检查，通过。
- 使用独立 User 18087（关闭 Nacos 注册及配置加载）、本机 MySQL/Redis，Auth 使用已有 8084。HTTP 验证昵称成功流水、旧版本 409 失败流水、禁用/启用/撤销重试成功流水、详情、过滤查询、无效枚举与分页 400、匿名 401、普通会员 403、禁用后原会员 Token 401。
- 联调新建会员 `2121156967710896129`，邮箱 `operation.verify.1794558204@example.com`，完成后保持禁用；管理员联调 Token 已退出。未修改初始普通会员资料或状态。
- 未新增或修改测试代码。未验证 Redis 故障下 PARTIAL、进程中断、浏览器交互、生产代理或新版服务 Nacos 负载均衡；构建和本机直连联调结果不代表这些场景已验证。

## 后台统一入口改造（2026-11-13）

管理请求现统一经过 Gateway → Admin → User/Goods，前端路径和响应格式不变。业务服务旧管理入口已移除，内部管理接口同时验证服务凭证与管理员会话。新版完整 HTTP 链路已手工验证；部署配置、实际验证范围与保留的验证数据见 [Admin 说明](admin.md)。上文更早的直连路由及未迁移状态记录仅代表对应历史验证。
