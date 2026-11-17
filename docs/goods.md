# 商品服务

`jaycong-business/jaycong-goods` 是独立 Spring Boot 应用，默认端口 `8085`，Nacos 服务名 `goods`，远程配置 Data ID 为 `goods.yml`。复用公共 Web、数据源和配置模块。

## 初始化和启动

在 MySQL 8 的目标数据库手工执行 `jaycong-business/jaycong-goods/src/main/resources/db/goods-mysql.sql`。应用不自动建表或写入数据。默认数据库 `jaycong-framework`，账号密码沿用本地开发默认值 `root`；可通过 `GOODS_DB_URL`、`GOODS_DB_USERNAME`、`GOODS_DB_PASSWORD` 覆盖。端口通过 `GOODS_PORT` 覆盖。Nacos 使用公共 `NACOS_*` 环境变量配置，启动前准备 Nacos 和 `goods.yml`。

```powershell
mvn -B -ntp -pl jaycong-business/jaycong-goods -am package -DskipTests
java -jar jaycong-business/jaycong-goods/target/jaycong-goods-0.1.0-SNAPSHOT.jar
```

## 分页查询

`GET /goods` 不要求登录，仅返回未删除且已上架的商品。网关默认可通过 `http://localhost:8090/goods` 访问；若 Nacos 中的 gateway.yml 覆盖 routes，需同步加入 `Path=/goods,/goods/**` 到 `lb://goods` 的路由。

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| page | 1 | 页码，至少 1 |
| size | 20 | 每页数量，1～1000 |
| name | 无 | 名称包含匹配，最多 128 字符，忽略首尾空白；% 和 _ 按字面匹配 |

按排序值升序，再按创建时间、ID 倒序；旧商品排序值默认 0，保留原有相对顺序。无匹配和超出总页数均返回空 rows。非法分页参数、参数类型或过长筛选条件返回 HTTP 400。数据库价格为整数分，接口转换为十进制元，ID 使用字符串避免 JavaScript 精度损失。LIKE 转义遵循 MySQL 默认 SQL 模式，不启用 NO_BACKSLASH_ESCAPES。

```powershell
curl.exe -G http://localhost:8085/goods --data-urlencode 'page=1' --data-urlencode 'size=20' --data-urlencode 'name=耳机'
curl.exe -i 'http://localhost:8085/goods?page=0'
```

成功响应示例（示例数据需手工准备）：

```json
{"success":true,"code":0,"message":"成功","data":{"total":1,"rows":[{"id":"1001","name":"无线蓝牙耳机","goodsCode":"EARBUDS-001","subtitle":"轻盈入耳","price":299.00,"image":"/images/earbuds.png","description":"轻盈入耳"}]}}
```

## 商品详情

`GET /goods/{id}` 返回单个未删除、已上架商品，data 结构与分页 rows 中的单项一致，price 单位为元。不存在、已删除或已下架返回 HTTP 404。

服务间读取统一使用 Common API 的 `GoodsFacade` 和 `GoodsFeignClient`，由 `GoodsFacadeServiceImpl` 提供 `GET /internal/goods/{id}`。内部接口成功直接返回已有 `GoodsView`，与公开详情复用 `GoodsReadService.getAvailable` 和 `GoodsConvertor`，保留同样的上架和逻辑删除筛选；失败由公共异常处理器返回对应 HTTP 状态。该只读接口返回公开资料，不要求用户登录，网关不公开此路由。订单服务通过此契约获取下单快照，调用配置见 [RPC 接入说明](rpc.md)。

公开接口保持只读；后台商品维护、库存调整、固定分类选择及 `mall-admin` 对接见 [后台单商品管理](product-management.md)。分类由 `GoodsCategory` 枚举维护，不新增分类表。新增商品由应用生成 BIGINT 主键，默认下架且库存为 0，状态 1 表示上架。已有数据库需手工执行管理功能升级脚本后再部署新版 Goods 服务。


## 商品模块分包

按用户指定的 `NFTurbo_Server/nft-turbo-business/nft-turbo-order` 实际代码组织，分别对照 `OrderReadService`、`OrderManageService`、`TradeOrder`、`TradeOrderConvertor`、`OrderMapper` 和 `OrderFacadeServiceImpl`。Goods 不额外拆分 application、repository、port 或独立数据库 PO。

```text
io.github.jaycong.goods
├── GoodsApplication
├── GoodsException
├── controller
│   ├── GoodsController
│   └── GoodsQueryRequest
├── facade
│   ├── GoodsFacadeServiceImpl
│   └── GoodsManagementFacadeServiceImpl
├── domain
│   ├── entity
│   │   ├── Goods
│   │   ├── GoodsDetails
│   │   ├── GoodsCategory
│   │   └── convertor/GoodsConvertor
│   └── service
│       ├── GoodsReadService
│       └── GoodsManageService
└── infrastructure
    ├── mapper/GoodsMapper
    └── config
        ├── AdminRpcConfiguration
        └── GoodsManagementConfiguration
```

- `controller`：公开 HTTP 路由和 Web 参数绑定；公开展示模型 `GoodsView` 位于 `jaycong-common-api` 的 `api.goods.model`，与参考项目的 API 展示模型放置方式一致。
- `facade`：实现 Common API 商品读取及内部管理契约，直接接收已有请求，调用读取或管理服务，将实体结果转换为协议响应。
- `domain.service`：`GoodsReadService` 负责公开及管理读取、筛选、稳定分页；`GoodsManageService` 通过 `AuthContext.requireAdmin()` 核验管理员，负责事务及写入，直接调用 Mapper，不再经过转发接口。
- `domain.entity`：`Goods` 继承公共 `BaseEntity`，同时对应 goods 表并维护编码不可变、上下架、库存、删除及版本规则；`GoodsDetails` 承载商品资料规范化和校验，`GoodsCategory` 表达固定分类。
- `domain.entity.convertor`：一个 `GoodsConvertor` 集中转换保存请求、公开展示和管理展示，保留字符串 ID、元金额以及管理结果的文本规范化，不承担查询或状态变更。
- `infrastructure.mapper`：MyBatis-Plus 数据访问，直接映射 Goods 实体。
- `infrastructure.config`：内部服务凭证校验、严格整数绑定、Feign 装配和操作人审计填充。
- 共享会话、实时管理员角色校验、身份 RPC 失败处理及账号失效会话撤销统一由 `jaycong-common-satoken` 的 `AuthContext` 实现；Goods 不再定义 SessionGoodsAdmin 或 security 包。UserFeignClient 仍由配置显式启用，供 AuthContext 在鉴权时使用。

商品业务字段不提供公共 setter。Goods 保留主键生成、审计填充、逻辑删除和 `@Version` 乐观锁，更新及删除仍比较期望版本，删除还要求当前下架；划线价置 null 时仍清空数据库字段。历史未分类商品允许读取，新增、编辑和上架时执行完整业务校验，库存及状态操作不顺带规范化历史资料。

业务失败统一使用根包 `GoodsException`，继承 `BusinessException`，交由 common-web 的 `GlobalExceptionHandler` 返回既有状态与公开消息。现有 HTTP/RPC 路由、字段、金额单位及数据库结构保持兼容。

## 2026-11-16 参考 NFTurbo Order 调整验证

- Goods 主源码由 36 个 Java 文件、24 个有代码的包精简为 15 个文件、9 个包（含根包）。原 GoodsView 迁入已有 Common API 模块，统一作为对外展示契约；现有 Common API 请求和管理响应字段未修改。
- 执行 `mvn -B -ntp -pl jaycong-business/jaycong-goods -am clean package -DskipTests`，BUILD SUCCESS；未新增或修改测试代码，未运行自动测试。
- 临时启动实际打包产物，端口 `18085`，使用进程参数提供本地 MySQL 配置、关闭 Nacos 配置及服务发现，不修改仓库运行配置。
- 手工 HTTP 验证公开分页、已有商品详情、超出总页数空 rows 及字面百分号筛选均返回预期结果，ID 和金额格式与调整前一致。
- 页码 0、小数状态及小数库存增量返回 400；不存在商品返回 404；管理调用缺少服务凭证返回 403，提供临时服务凭证但未登录返回 401，确认 Facade 改名后契约路由仍生效。
- 未联调管理员登录后的 CRUD、审计写入及并发更新，需要 Redis、User 服务和有效管理员会话。验证未写入数据库，完成后停止临时 Goods 进程。
- 已核对资源文件、数据库脚本和测试目录与本轮调整前的备份一致。

后续将 SessionGoodsAdmin 移入公共 AuthContext 后，Goods 主源码为 14 个 Java 文件、8 个有代码的包（含根包）。Auth、Admin、User、Goods、Order 及其依赖完成干净构建；Goods、Order 临时启动及匿名访问、会话存储故障验证见[实施记录](superpowers/plans/2026-11-16-goods-ddd-refactor.md#后续统一-authcontext-鉴权)。实时角色授权及账号失效会话撤销尚未联调，未新增或修改测试代码。
