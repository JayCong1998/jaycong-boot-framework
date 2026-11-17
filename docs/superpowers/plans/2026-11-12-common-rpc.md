# Common RPC 实施计划

**目标：** 新增公共 RPC 依赖模块，并由 goods、order、pay 引用。

**方案：** 继承 Common 父 POM，组合官方 OpenFeign、LoadBalancer Starter，
复用现有 BOM 的 Spring Cloud 版本。业务显式启用 Client，不迁移现有调用。

- [x] 新增 `jaycong-common/jaycong-common-rpc/pom.xml`，声明两个官方 Starter。
- [x] 在 `jaycong-common/pom.xml` 聚合，在 `jaycong-bom/pom.xml` 管理模块版本。
- [x] 三个业务模块 POM 分别声明 `jaycong-common-rpc`，不重复指定版本。
- [x] 更新 README、Common 文档，新增 `docs/rpc.md` 说明扫描、发现与超时配置。
- [x] 执行 `mvn -B -ntp -pl :jaycong-goods,:jaycong-order,:jaycong-pay -am package -DskipTests`，13 个模块全部 SUCCESS，结果为 BUILD SUCCESS。

遵守项目约定，不新增或修改测试代码；本次不启动外部服务验证远程调用。
