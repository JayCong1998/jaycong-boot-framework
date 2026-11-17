# Goods 参照 NFTurbo Order 精简计划

**依据：** 用户最新指定 `NFTurbo_Server/nft-turbo-business/nft-turbo-order` 的实际实现覆盖上一版拆层方案。参考类为 `OrderReadService`、`OrderManageService`、`TradeOrder`、`TradeOrderConvertor` 和 `OrderMapper`。

**结构：** controller / facade → domain.service → infrastructure.mapper；Goods 同时承载实体行为与表映射，转换集中到 domain.entity.convertor。现有 Common API 请求和响应继续复用，不再复制内部命令、查询或结果。

**保留：** 构造器注入、实体业务方法、公共异常处理、审计填充、逻辑删除和乐观锁；现有 HTTP/RPC 路径、字段、ID 和金额格式保持兼容。当前不存在的 MQ、任务或状态机功能不创建对应空包。

- [x] 将 Goods 与 GoodsRecord 合并为 domain.entity.Goods；保留 GoodsDetails 的资料规范化与校验，GoodsCategory 为固定分类。
- [x] 建立 domain.service.GoodsReadService / GoodsManageService，分别负责查询及管理操作，直接使用 infrastructure.mapper.GoodsMapper；实体负责编码、上架、库存及删除规则。
- [x] 将多处对象转换合并为 domain.entity.convertor.GoodsConvertor；管理服务直接接受已有 Common API 请求，返回实体，由入口转换输出。
- [x] 移除 application、repository、独立 PO、端口及中间 DTO；HTTP 参数留在 controller，公开展示模型归入 Common API，基础设施统一使用 infrastructure 命名。
- [x] 按后续要求移除 infrastructure.security.SessionGoodsAdmin，登录及实时角色核验统一通过 common-satoken 的 AuthContext；商品业务异常继续使用根包 GoodsException，交给 common-web。
- [x] 更新 AGENTS.md 和 docs/goods.md 的 Goods 分层说明；按临时备份核对资源与测试代码没有改动。
- [x] 执行 Maven clean package -DskipTests，临时启动应用验证公开查询、参数错误及管理未授权请求，停止临时进程。管理员写入和并发流程按实际环境记录验证范围。

**工作区保护：** 当前全部 Goods 源码已备份到系统临时目录 `jaycong-goods-nfturbo-lvq0mye3`，不变更 Git 暂存区，不新增或修改测试代码。

**验证结果：** 干净构建 BUILD SUCCESS。实际打包应用启动成功，公开查询/详情/筛选/空页、400/404 参数错误和 403/401 管理访问拒绝均已通过 HTTP 手工验证。管理员写入和并发更新未联调；未写入数据库，已停止临时进程。Goods 资源及测试目录与本轮备份一致。

## 后续：统一 AuthContext 鉴权

- 移除 SessionGoodsAdmin 和空 security 目录；GoodsManageService 的所有管理操作直接调用 AuthContext.requireAdmin()。
- common-satoken 新增 requireRole/requireAdmin，实时查询 User 有效账号及角色；保留账号失效时撤销会话、权限不足返回 403、核验失败返回 503 的处理。UserFeignClient 延迟获取，不强制仅使用会话读取的应用启用客户端。
- 执行 `mvn -B -ntp -pl jaycong-auth,jaycong-admin,jaycong-business/jaycong-user,jaycong-business/jaycong-goods,jaycong-business/jaycong-order -am clean package -DskipTests`，17 个聚合及业务模块全部构建成功；未新增、修改或运行自动测试。
- 临时启动 Goods（18085）和未启用 UserFeignClient 的 Order（18086）均成功。Goods 公开分页返回 200；管理调用缺少服务凭证返回 403，带服务凭证但未登录返回 401；Order 匿名下单返回 401。
- Redis 未运行时，Goods 携带 Token 的管理调用返回 503，未降级为匿名或权限不足。Redis、User 服务及有效会话未就绪，未联调实时管理员放行、普通用户拒绝和账号失效会话撤销。
- 验证没有写入数据库，临时进程均已停止，18085/18086 不再监听。代码调整前备份位于系统临时目录 `jaycong-authcontext-authorization-7ccjnw0p`。
