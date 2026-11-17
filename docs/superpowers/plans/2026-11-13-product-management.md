# 单商品管理实施计划

**目标：** 在现有 goods 服务和 mall-admin 中实现单商品维护、一级分类、价格、独立库存调整、上下架、详情及逻辑删除，不增加 SKU 或流水表。

**架构：** 复用 goods 表及公开查询协议，新增管理接口 `/admin/goods` 与分类只读接口 `/admin/goods-categories`。分类统一由后端 `GoodsCategory` 枚举维护，不增加分类表。管理接口复用 Auth 的 Redis 会话，并通过 User Facade 实时核验管理员身份。领域模型和规则不依赖持久化实现。金额接口为元、存储为分，ID 为字符串。所有商品修改进行乐观锁检查。

## 任务

- [x] 后端：新增领域模型、规则、仓储接口、应用编排、管理 Controller 与请求校验；MyBatis 持久化、分页、唯一编码冲突处理、版本冲突处理；固定分类枚举及只读接口；增加共享认证配置。
- [x] 数据库：更新全新安装脚本，提供既有 goods 表独立升级脚本；只扩展商品表，不增加分类表；不自动执行 SQL，不修改用户数据。分类未分配的旧商品可正常读取，维护上架时需选有效固定分类。
- [x] 前端：新增 API 类型、商品列表、筛选、编辑、详情、上下架、删除和库存调整弹窗；读取后端固定分类，不提供分类维护页面；接入路由和代理；低库存筛选接通工作台。
- [x] 集成：网关新增管理路径路由，更新运行和部署说明，明确图片 URL 录入、纯文本详情、库存不留流水的边界。
- [x] 验证：运行 `npm run typecheck`、`npm run build`、匹配模块的 Maven package（跳过测试）；服务条件允许时通过 HTTP 手工验证授权和业务流程。按用户约定不创建或修改测试代码。代码审查并修复问题后交付，报告实际验证和待执行 SQL。

## 接口契约

商品字段：`id, goodsCode, name, subtitle, category, coverUrl,  description, price, originalPrice, stock, stockWarning, sortOrder, status, lockVersion, createTime, updateTime`。

`GET /admin/goods` 参数 `page,size,name,goodsCode,category,status,lowStock`，返回 `PageResult`。`GET /admin/goods/{id}` 返回详情。`POST /admin/goods` 创建下架商品、初始库存为 0；`PUT /admin/goods/{id}` 编辑资料（不含库存与状态）。售价必须大于 0，划线价可为 null，否则不低于售价。`PUT /admin/goods/{id}/status` 接收 `{status,lockVersion}`；`PUT /admin/goods/{id}/stock` 接收 `{quantity,lockVersion}`（有符号增量，非零）；`DELETE /admin/goods/{id}?lockVersion=...` 只允许下架商品。

`GET /admin/goods-categories` 返回枚举中的固定分类数组，字段 `value,label`，两者均为中文分类名；无分类写接口和数据库访问。商品 category 直接保存中文，新增、编辑、上架、筛选时校验值合法性。不包含轮播图字段或功能，仅保留商品主图。

所有成功响应复用 `Result`；400 参数错误、401 未登录、403 非管理员、404 不存在、409 版本或业务状态冲突。图片为最多 512 字符的 http(s) URL 或以单斜杠开头的站内路径，详情为纯文本，避免富文本注入。

