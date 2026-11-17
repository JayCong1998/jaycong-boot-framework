# 商品分页查询设计

用户已选择商品分页查询方案。将现有 jaycong-business/jaycong-goods 初始化为独立服务，默认端口 8085，Nacos 服务名 goods，配置 goods.yml，网关保留 /goods 路径转发。

GET /goods 使用 page（默认 1）、size（默认 20，最大 1000）、name（最多 128 字符，字面包含匹配）。空白筛选条件忽略。仅返回 status=1 且 deleted=0 的商品，按 create_time、id 倒序。超出总页数返回空列表。

返回公共 Result<PageResult<GoodsView>>；商品公开字段为字符串 id、goodsCode、name、subtitle、十进制 price（由数据库整数分转换为元）、image、description。参数错误返回 HTTP 400，由公共 Web 模块处理；数据库异常使用现有统一异常处理。

复用本地已有 MySQL goods 表，不修改数据库结构；封面 cover_url 映射为接口 image，暂不提供分类筛选。表包含标准审计、逻辑删除和版本字段；商品实体复用 BaseEntity，分页、审计填充和乐观锁复用 common-datasource。不增加写接口、库存、分类表、登录限制或前端对接。

遵守项目约定，不新增或修改测试代码。通过 Maven package -DskipTests、可用条件下的应用启动和 HTTP 手工调用验证。建表手工执行，禁止启动自动初始化。

