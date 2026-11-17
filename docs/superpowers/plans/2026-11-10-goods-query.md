# 商品分页查询实现计划

**Goal:** 初始化 goods 服务并提供可筛选的商品分页查询。

**Architecture:** GoodsController 绑定并校验 GoodsQuery，GoodsService 通过 GoodsMapper 和公共分页插件查询 MySQL。GoodsView 隔离内部审计字段。

**Tech Stack:** Java 21、Spring Boot 3.5、MyBatis-Plus、MySQL 8、Nacos。

- [x] 为 jaycong-business/jaycong-goods/pom.xml 添加 common-config、common-datasource 和 Boot repackage，新增 GoodsApplication、application.yml、bootstrap.yml，沿用现有版本管理。
- [x] 在 io.github.jaycong.goods 下增加 product/Goods、GoodsMapper，api/GoodsQuery、GoodsView，service/GoodsService，controller/GoodsController；校验 page>=1、size 1..1000 及筛选长度；固定过滤已上架未删除商品并稳定倒序。
- [x] 新增 src/main/resources/db/goods-mysql.sql 和 docs/goods.md，说明手工建表及查询；网关新增 lb://goods 路由。现有业务聚合已经注册 goods，无需修改根 POM 或 BOM。
- [x] 执行 mvn -B -ntp -pl jaycong-business/jaycong-goods,jaycong-gateway -am package -DskipTests；检查可用服务并在条件允许时启动验证默认分页、筛选、空结果和非法参数。不得编写或修改测试代码。

## 实际验证

Maven 打包成功；goods 应用启动并注册到本地 Nacos。HTTP 手工验证：分页 size=2 返回 total=7 和 2 行；名称筛选返回 1 行；无匹配和超大页码返回空行；字面 % 无匹配；page=0、size=1001、page=abc 返回 400。发现本地已有 goods 表后按其结构适配，无数据库写入。网关转发未实际验证。验证结束已停止本次启动的应用。
