# Admin 后台管理统一入口

`jaycong-admin` 位于根目录，与 Auth、Gateway 同级，默认端口 `8088`（`ADMIN_PORT` 可覆盖）。它是后台管理唯一 HTTP 入口，不访问数据库，不作为外部依赖加入 BOM。

## 调用链路与职责

```text
mall-admin → Gateway /admin/** → Admin Controller → Admin Service → Feign
  → User /internal/admin/members/** → User application → domain / infra.repository
  → Goods /internal/admin/goods/**、/internal/admin/goods-categories → Goods application → domain / infra.repository
```

会员分页/详情/昵称/状态、会员操作记录、商品分页/详情/创建/资料/上下架/库存/删除、固定分类接口均保持原 `/admin/**` 路径、参数及 Result 响应。前端无需调整请求地址。User、Goods 的原 `/admin/**` 入口已移除，不能继续直连旧地址。

Admin 每次请求使用共享 Redis 会话取得账号 ID，通过 User RPC 查询实时账号有效性和 `admin` 角色。身份无效返回 401，普通用户返回 403。内部管理客户端仅透传当前经过校验的 Authorization，不接收客户端声明的操作人或内部凭证；业务服务再次校验会话与角色，审计保留真实管理员身份。

业务规则、数据库事务、版本检查、会员会话撤销和操作流水仍由对应业务服务执行。远程契约位于 Common API 的 `user.management`、`goods.management`，不依赖应用 JAR。内部成功响应为业务数据，Admin 包装为原 Result；明确的业务错误保留 HTTP 400/401/403/404/409，网络、内部凭证及非协议错误返回 503。写调用不自动重试；超时后先刷新资料、库存和流水确认结果。

## 必需配置

- Nacos：服务名 `admin`，Config Data 加载 `admin.yml`，公共连接参数为 `NACOS_*`。
- Admin、User、Goods 都配置相同的 `ADMIN_SERVICE_TOKEN`，绑定到 `jaycong.rpc.admin.token`。内部管理请求使用 `X-Admin-Service-Token`，未配置或不匹配即拒绝；这个凭证不放入前端代码、浏览器请求或仓库。
- Redis 使用 `AUTH_REDIS_HOST`、`AUTH_REDIS_PORT`、`AUTH_REDIS_PASSWORD`、`AUTH_REDIS_DATABASE`，各服务与 Auth 保持一致；loginType 为 `jaycong-auth`，请求头为 `Authorization: Bearer <token>`。
- `USER_SERVICE_TOKEN` 属于原认证 User 客户端兼容配置，不替代新的管理服务凭证；原 `/internal/users/**` 行为本次未改变。
- Feign 默认通过 Nacos 服务名 `user`、`goods` 寻址。各客户端配置 connectTimeout 2000ms、readTimeout 5000ms，禁用请求日志和自动业务重试。

管理客户端 contextId：`adminMemberManagement`、`adminMemberOperationManagement`、`adminGoodsManagement`。本地直连可使用 Spring 原生 `spring.cloud.openfeign.client.config.<contextId>.url` 覆盖目标，账号核验客户端为 `authUser`。

## 网关与发布

Gateway 的 `/admin,/admin/**` 统一转发 `lb://admin`，公开 `/goods,/goods/**` 仍到 Goods，`/auth,/auth/**` 仍到 Auth。内部 `/internal/**` 不配置网关路由。聚合文档 `/openapi/admin` 替换原 `/openapi/user`，Admin 文档只包含管理 Controller。

若 Nacos 的 `gateway.yml` 覆盖 routes，需同步删除原直达 User/Goods 的管理路由，新增 Admin 路由；同时同步 `admin.yml`、`user.yml`、`goods.yml` 的凭证与连接配置。重启加载新版 Admin、User、Goods、Gateway；不可只更新网关而保留旧服务入口。生产环境仅向 Gateway 暴露业务流量，内部服务端口保持内网访问。

本次不修改数据库结构、不执行 SQL，商城商品查询与订单链路保持原接口。已有数据库须已完成商品及会员操作流水表的手工建表/升级。

```powershell
mvn -B -ntp -pl jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-gateway -am package -DskipTests
java -jar jaycong-admin/target/jaycong-admin-0.1.0-SNAPSHOT.jar
```

健康检查：`GET /actuator/health`。后台账号登录、退出仍由 Auth 的 `/auth/**` 承担。

## 2026-11-13 实际验证

- Maven 执行 `mvn -B -ntp -pl jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-gateway -am clean package -DskipTests` 成功；随后含 Auth 的定向 package 成功，未新增或修改测试代码。
- 在 19084/19085/19087/19088/19090 临时端口启动新版 Auth、Goods、User、Admin、Gateway；使用现有 MySQL、Redis，禁用验证进程的 Nacos 配置与注册，Feign/网关目标临时改为本地直连。未替换用户已有进程或修改 Nacos。
- 通过 Gateway → Admin → User 验证会员分页、时间筛选、详情、昵称修改、启用/禁用、旧版本 409、禁用撤销会话及禁止新登录、重新启用后旧 Token 仍无效；操作记录包含真实管理员 ID `1`、来源 `ADMIN_WEB` 和成功结果。
- 通过 Gateway → Admin → Goods 验证分页、分类、创建、资料更新、库存增量、负库存拒绝、过期版本 409、上下架、上架商品禁止删除、逻辑删除后不可查询；通过原公开 `/goods/{id}` 读取上架商品成功。
- 匿名请求 401、普通用户 403；User/Goods 原 `/admin/**` 入口 404；内部缺少或伪造凭证 403，只有正确凭证但无会话仍返回 401；Gateway 不暴露内部路径。非法分页、小数库存 400；停止 Goods 后管理调用返回 503。Admin OpenAPI 经过网关读取成功。
- 验证商品 `2121175117080838145` 已逻辑删除。验证会员 `2121175111343026178`（`admin.rpc.verify.1794562530474711700@example.com`）保留为禁用状态，其操作流水保留；未修改初始会员资料或状态。登录验证会更新初始账号的最后登录时间和版本号。验证创建的登录会话已退出，临时进程均已停止。
- 本次未验证新版服务通过 Nacos 负载均衡的整链路、生产代理部署或浏览器页面交互。前端协议未改动，无需同步修改前端代码。

## 与 Auth 对齐的分包及调用方式

Admin 按用户要求采用与 Auth 一致的入口服务分包：`controller`、`service`、`request`、`response`、`config`、`exception`。业务服务 User/Goods 继续保留领域分层。

Controller 接收 Admin 本地 request，调用 MemberService、MemberOperationService 或 GoodsService，由 Service 转换为 Common API 请求并调用显式启用的 Feign 客户端。返回模型在 Service 转换为 Admin 本地 response，HTTP 协议与 RPC 契约分别维护。

UserService 封装账号核验的 UserFeignClient；AdminAccessService 编排账号与会话校验。AdminRemoteService 集中处理管理 RPC 错误及空响应，转换为继承 BusinessException 的 AdminException；common-web 的 GlobalExceptionHandler 统一将本地异常映射成 HTTP Result，不依赖 Feign，Admin 不声明独立异常处理器。

本轮结构调整后执行 `mvn -B -ntp -pl jaycong-admin -am clean package -DskipTests` 成功。临时端口启动五个服务，经 Gateway 验证会员/操作流水/商品分页及分类、时间筛选、400/401/403/404、业务服务不可用 503 和 OpenAPI；22 项 HTTP 状态检查全部通过。另确认商品分页正常返回 200，Admin 产物不包含旧 application/infra 包。未重复创建或修改业务验证数据；登录更新账号登录时间，验证会话已退出，临时进程已停止。本轮未重新验证成功写入及 Nacos 负载均衡。
