# Auth 与 Goods DDD 分包实施计划

**目标：** 按项目 DDD 约定重组现有认证、会员和商品功能，保留 HTTP 协议及业务行为。

**结构：** controller 接收请求；application.service 编排用例；domain.model / domain.service 定义领域数据及业务规则；infrastructure.repository / persistence / config 定义持久化接口并提供技术实现。

- [x] 移动 Controller、请求响应、配置和持久化对象，更新引用。
- [x] 抽出纯领域用户模型和商品模型，通过 infrastructure 中的仓储接口隔离 MyBatis 查询和更新。
- [x] 应用服务使用用例参数和结果，不依赖 controller 或持久化实现。
- [x] 检查旧包引用、分层依赖及 HTTP 字段，执行 Maven 编译、尝试打包并记录环境限制。

遵循项目约定，不新增或修改测试代码，不创建未使用的入口层。

## 实际验证

- `mvn -B -ntp -pl jaycong-auth,jaycong-business/jaycong-goods -am compile -DskipTests`：成功。
- `mvn -B -ntp -pl jaycong-auth,jaycong-business/jaycong-goods -am clean package -DskipTests`：auth 原有 JAR 被其他进程占用，clean 无法删除；保留现有进程，没有强行终止。
- 使用编译后的新类及现有依赖，在临时端口 18084、18085 启动两个应用；验证时使用临时本地配置，关闭 Nacos，不验证注册发现。
- auth：`GET /auth/status` → 200；`GET /auth/me`、`GET /admin/members` → 401；无效账号 `POST /auth/login` → 401，已覆盖仓储登录查询。
- goods：`GET /goods?page=1&size=2` → 200，返回数据库商品；`page=0` → 400；不存在的商品详情 → 404。
- 验证进程已停止。未执行注册、有效登录或会员更新，未新增或修改测试代码。

后续按用户要求，auth、goods 的仓储接口统一移入 `infrastructure.repository`；domain 不定义数据库操作。
