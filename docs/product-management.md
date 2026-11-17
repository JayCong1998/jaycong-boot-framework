# 后台单商品管理

管理端 `/products` 复用现有 Goods 服务，不引入 SKU 或库存流水。每个商品独立维护价格、库存、预警阈值、固定分类、主图、纯文本详情及上下架状态。商品编码创建后不可修改，逻辑删除后仍占用唯一编码。

## 固定分类

分类由 `domain/entity/GoodsCategory.java` 枚举统一维护，前端通过只读接口获取，不新增分类表，不提供分类增改、排序和启停用入口。固定值为：手机数码、电脑办公、家用电器、家居生活、服饰鞋包、其他商品。商品表使用 `category VARCHAR(64)` 直接保存中文；写入和筛选时后端校验合法性。分类名称就是持久化值，重命名时需同步迁移已有数据。

## 数据库变更（需要手工执行）

当前脚本仅增加中文 `category`、划线价、预警阈值和排序字段，不创建分类表或轮播图字段。若已执行过之前含 `category_id` 的版本，需改用 `goods-category-text-upgrade.sql` 新增中文字段并转换固定编号；不要重复执行初始管理升级脚本。旧 `category_id`、`gallery_urls` 列可保留不用，不自动删除或丢弃数据。来自旧自定义分类表的编号需先人工核对含义，再决定是否采用该转换脚本。

应用不会自动执行 SQL。本次不连接数据库执行迁移。

- 已存在 `goods` 表：先备份，再执行 `jaycong-business/jaycong-goods/src/main/resources/db/goods-admin-upgrade.sql`，只执行一次。
- 全新数据库：执行更新后的 `jaycong-business/jaycong-goods/src/main/resources/db/goods-mysql.sql`，不要再执行升级脚本。
- 原有商品不自动分配分类；首次维护时选择有效的固定分类。原有价格、库存、状态和订单快照不作迁移改写。

## 启动与配置

Goods 默认端口 `8085`，管理 HTTP 入口由 Admin（默认 `8088`）统一提供。Admin 通过内部 Goods Facade 调用应用用例；Goods 校验服务凭证、共享登录会话及实时管理员角色，持有数据库、事务、库存规则和审计填充。Admin、User、Goods 配置相同的 `ADMIN_SERVICE_TOKEN`，Redis 配置与 Auth 一致，详见 [Admin 说明](admin.md)。
```powershell
mvn -B -ntp -pl jaycong-admin,jaycong-business/jaycong-goods,jaycong-business/jaycong-user,jaycong-gateway -am package -DskipTests
java -jar jaycong-business/jaycong-goods/target/jaycong-goods-0.1.0-SNAPSHOT.jar
```

Gateway 的 Goods 路由仅保留 `/goods,/goods/**`，`/admin,/admin/**` 全部指向 `lb://admin`。若 Nacos 覆盖 routes，必须同步删除原直达 Goods/User 的管理路由并更新 Admin 路由。前端 Vite/Nginx 仍代理现有 `/admin/` 路径。升级需部署新版 Admin、Goods、User 和 Gateway。

## 管理操作

商品列表支持名称包含、商品编码、分类、上下架状态及低库存筛选，默认每页 20 条。工作台“库存预警商品”跳转会启用低库存筛选；工作台预警数量仍是标注的示例数据。低库存指 `stock <= stockWarning`，包含下架商品。

新商品默认下架、库存为 0。编辑资料不修改库存与上下架状态。仅保留一张商品主图，主图录入 http(s) URL 或站内路径，本次不提供文件上传、对象存储和富文本编辑器。详情按纯文本显示。

库存调整提交有符号整数增量：正数增加、负数减少，不能为 0，最终库存不得为负或超过 INT 上限。没有库存流水表，因此不收集无法持久化的调整原因。网络超时后先重新读取库存，确认结果再操作，不自动重试写请求。

售价大于 0，划线价可为空，否则不能低于售价；接口金额为元，数据库为整数分。商品 ID 使用字符串，避免 BIGINT 精度丢失；category 直接传递和保存中文分类值。

上架需有效名称、主图、价格、合法的固定分类等资料。删除只允许下架商品，采用逻辑删除，保留行及 ID，不影响订单已保存的商品快照。当前订单服务没有后台订单关联查询，本次不增加订单维护行为。

## 接口

| 方法与路径 | 用途 |
| --- | --- |
| `GET /admin/goods` | 商品分页，参数 page、size、name、goodsCode、category、status、lowStock |
| `GET /admin/goods/{id}` | 商品详情 |
| `POST /admin/goods` | 创建下架商品，初始库存 0 |
| `PUT /admin/goods/{id}` | 更新资料，包含 lockVersion |
| `PUT /admin/goods/{id}/status` | `{status, lockVersion}`，0 下架、1 上架 |
| `PUT /admin/goods/{id}/stock` | `{quantity, lockVersion}`，quantity 为库存增量 |
| `DELETE /admin/goods/{id}?lockVersion=...` | 逻辑删除下架商品 |
| `GET /admin/goods-categories` | 返回固定分类数组，每项为 value、label，无数据库查询 |

接口使用统一 Result；未登录 401、无权限 403、参数错误 400、不存在 404、并发或业务冲突 409。版本冲突后必须重新读取、核对资料后再提交，前端不会静默覆盖。分类为常量，不涉及分类行锁或外键。

公开 `GET /goods` 和 `GET /goods/{id}` 保持现有响应协议，仍只返回未删除且上架的商品；本次不改动订单成交金额或库存扣减流程。

## 手工验收建议

按项目约定不新增或修改测试代码。迁移后依次验证管理员读取固定分类、新建商品、调整库存、上架、筛选、编辑、下架和删除；确认普通用户管理请求 403、未登录 401、过期版本 409、负库存拒绝、商品编码重复拒绝及非法分类值返回 400。检查公开查询仍可被商城与订单服务使用。

## 2026-11-13 实际验证

中文分类及移除轮播图后，重新执行 `mvn -B -ntp -pl jaycong-business/jaycong-goods -am package -DskipTests` 和前端 `npm run build`（含类型检查）均成功。该次调整尚未执行 SQL 或验证数据库读写流程。



- `mall-admin` 执行 `npm run typecheck`、`npm run build` 成功。
- 根目录执行 `mvn -B -ntp -pl jaycong-admin,jaycong-business/jaycong-goods,jaycong-business/jaycong-user,jaycong-gateway -am package -DskipTests` 成功，跳过测试。
- 临时启动新版 Goods，关闭该验证进程的 Nacos 注册，确认 Spring 应用启动成功。验证结束后停止该进程，未重启用户已有 Auth、User、Gateway、Vite 服务。
- HTTP 验证匿名查询及无效 Token 返回 401；普通用户管理请求返回 403；管理员访问负数商品 ID 返回 404，确认共享会话与 User RPC 核验链路可用。验证使用的 HTTP 登录会话均已退出。
- 非法分页、小数库存增量和小数状态输入返回 400，不会静默转换为整数。
- 浏览器确认管理员登录回跳 `/products`、商品筛选页面和新增商品表单可打开；旧网关进程尚无新增管理路由，页面正确显示请求失败及重试入口。
- 数据库尚未迁移。管理员分页请求返回 500，日志确认 `Unknown column 'category'`；未验证依赖新增字段的 CRUD、库存并发及完整前后端数据流程。执行升级脚本并启动新版 Goods、重启新版网关后再验收这些流程。

## 后台统一入口改造（2026-11-13）

管理请求现统一经过 Gateway → Admin → User/Goods，前端路径和响应格式不变。业务服务旧管理入口已移除，内部管理接口同时验证服务凭证与管理员会话。新版完整 HTTP 链路已手工验证；部署配置、实际验证范围与保留的验证数据见 [Admin 说明](admin.md)。上文更早的直连路由及未迁移状态记录仅代表对应历史验证。
