# Common Config 设计

用户已确认采用服务发现，其余按推荐方案实施。

- 在 Common 下新增普通 JAR `jaycong-common-config`，组合官方 Nacos Discovery 和 Config Starter，不引入 MVC 或自定义扫描。
- BOM 统一管理 Spring Cloud Alibaba 2025.0.0.0，与当前 Boot 3.5、Cloud 2025.0 系列配合。
- 公共连接配置通过显式 classpath import 使用；Auth、Gateway 分别加载各自的 Nacos YAML Data ID，支持环境变量连接参数。
- Gateway 单独引入 LoadBalancer，Auth 路由默认使用 `lb://auth`，保留 AUTH_BASE_URL 覆盖。
- 更新模块及运维说明，以 Maven package、启动日志和条件允许时的 HTTP 请求验证。不新增或修改测试，不提交现有用户改动。
