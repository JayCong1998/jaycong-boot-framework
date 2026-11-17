# 会员管理

mall-admin 的 `/members` 提供真实会员分页、关键词/ID/状态/注册时间筛选、详情、昵称编辑和启用/禁用。会员指 `sys_user.role=user`，不管理管理员。HTTP 入口统一由 Admin 提供，再远程调用 jaycong-business/jaycong-user；业务数据、状态变更及审计留在 User，无需变更数据库结构。

## 接口

所有接口要求 `Authorization: Bearer <tokenValue>`，服务端每次查询当前账号状态和角色，仅允许正常管理员。响应使用 `Result`，会员 ID 为字符串；手机号、邮箱在列表和详情中均脱敏，不返回密码哈希。

| 方法 | 路径 | 参数 |
| --- | --- | --- |
| GET | `/admin/members` | `page=1`、`size=20`（1～100）、`keyword`、`id`、`status`（0 禁用、1 正常）、`createTimeStart`、`createTimeEnd` |
| GET | `/admin/members/{id}` | 会员 ID |
| PUT | `/admin/members/{id}` | `{ "nickname": "新昵称", "lockVersion": 1 }` |
| PUT | `/admin/members/{id}/status` | `{ "status": 0, "lockVersion": 2 }` |

时间参数使用 `2026-11-12T00:00:00` 格式，按服务端本地时间解释，范围包含两端。列表按注册时间、ID 倒序，返回 `data: { total, rows }`；关键词对昵称、手机号、邮箱进行字面值包含搜索，ID 精确查询，所有条件组合生效。默认排除逻辑删除用户。

会员字段：`id`、`nickname`、`mobile`、`email`、`status`、`createTime`、`updateTime`、`lastLoginTime`、`lockVersion`。昵称非空且最多 64 个字符。后台不修改登录标识、角色或密码。

更新携带读取时的版本号，由 MyBatis-Plus 乐观锁检查；发生并发变更（包括会员登录）返回 HTTP 409，重新查询后再操作。匿名返回 401，普通用户返回 403，查询管理员 ID 或不存在/已删除会员返回 404。

禁用先提交数据库，再撤销该账号全部 Sa-Token 会话。Redis 故障时接口报错，不宣称成功；刷新可看到已禁用状态，并在详情中“重试撤销会话”。启用前也撤销残留会话，撤销失败不会启用。登录增加签发会话后的状态检查，避免并发禁用后遗留有效登录。数据库和 Redis 不使用分布式事务，故障期间仍需按上述方式重试。

## 页面数据与 HTTP 约定

详情每次打开或点击“刷新资料”均 GET 读取最新数据；编辑弹窗也独立 GET 获取昵称和 lockVersion，不复制列表旧快照。读取期间禁止保存，失败提供重新加载。关闭弹窗、切换对象后忽略迟到响应。更新完成后重新读取列表及已打开的详情。

管理端统一按查询 GET、创建 POST、更新 PUT、删除 DELETE 选择请求方法。会员资料和状态是受限的可编辑资源，PUT 提交其完整可写字段及乐观锁版本，不开放手机号、邮箱、角色、密码编辑。当前没有创建或删除会员入口，不添加无业务用途的接口。

升级时需同步发布 mall-admin 和 User，旧 PATCH 更新方式不再支持。管理端退出调用 DELETE /auth/session，Auth 保留 POST /auth/logout 兼容现有商城。

## 路由与部署

Gateway 的 `/admin/**` 统一转发 `lb://admin`，Admin 再调用 User 内部管理 Facade。Vite 和 Nginx 的原路径不变。发布时同步更新 Admin、User、Goods、Gateway；Nacos 若覆盖路由，必须移除原直达业务服务的管理路由。Admin、User、Goods 配置相同 `ADMIN_SERVICE_TOKEN`，并共享 Auth 的 Redis 会话。完整部署说明见 [Admin](admin.md)。

生产环境在原有 `/auth/` 代理外增加：

```nginx
location /admin/members {
    proxy_pass http://127.0.0.1:8090;
}
```

会员操作审计流水见 [操作记录说明](member-operations.md)。不新增测试代码。保留数据库原有审计字段填充及乐观锁。订单、积分、等级、优惠券、删除、代建账号和密码重置不属于本期。

## 本次验证

2026-11-12：前端 `npm run typecheck`、`npm run build` 通过。后端 `mvn -B -ntp -pl jaycong-auth,jaycong-gateway -am package -DskipTests '-Dspring-boot.repackage.skip=true'` 通过；默认 repackage 因正在运行的 JAR 被 Windows 占用失败，另用临时模块 POM 设置独立 finalName 打包可执行 JAR，原始 POM 未改变。

在 18084 独立启动新版 Auth（禁用 Nacos 服务注册），使用本机 MySQL/Redis 手工 HTTP 验证：管理员分页/详情、昵称更新、409 版本冲突、匿名 401、普通账号 403、管理员对象 404、禁用后 Token 401/新登录 401、启用后旧 Token 401/重新登录成功。

独立启动 Gateway 18090，将临时配置中的 Auth 目标替换为 18084，沿用代码中的路径谓词；实际通过 Gateway 及 Vite 15174 代理读取会员列表成功，关键词/状态组合筛选成功，无效分页、状态及反向时间范围均返回 400。未验证生产 Nginx 部署或新版服务的 Nacos 负载均衡注册。

联调新建会员 ID `2120795651302838274`，邮箱 `member.verify.1794472059@example.com`，完成验证后保持禁用，其会话已撤销；未改动初始普通会员资料或状态。浏览器确认未登录访问 `/members` 跳转登录；随后浏览器会话丢失，未完成登录后的页面交互和移动端视觉验证。

### RESTful 优化验证（2026-11-12）

`npm run build`（mall-admin，包含 TypeScript 检查）通过；`mvn -B -ntp -pl jaycong-business/jaycong-user,jaycong-auth -am package -DskipTests -Dspring-boot.repackage.skip=true` 通过。本次未进行新版服务启动、HTTP 或浏览器联调，未新增或修改测试代码。前述历史联调结果不代表本次新增接口已经联调。

## 后台统一入口改造（2026-11-13）

管理请求现统一经过 Gateway → Admin → User/Goods，前端路径和响应格式不变。业务服务旧管理入口已移除，内部管理接口同时验证服务凭证与管理员会话。新版完整 HTTP 链路已手工验证；部署配置、实际验证范围与保留的验证数据见 [Admin 说明](admin.md)。上文更早的直连路由及未迁移状态记录仅代表对应历史验证。
