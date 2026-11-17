# Common 模块迁移计划

目标：将四个现有基础模块迁入 jaycong-common 并统一 Maven 坐标，保留 Java 包名及业务行为。

- 创建 packaging=pom 的 jaycong-common，继承根 POM，聚合四个公共模块。
- 移动 Core、Context、Web、MyBatis；子模块继承 common，更新 artifactId 和内部依赖。
- 更新根聚合、独立 BOM、各消费模块 POM、当前使用文档及 AGENTS.md；历史设计计划保留原貌。
- 不新增或修改测试代码，不引入尚未实现的模块，不移动业务和学习模块。
- 执行 mvn -B -ntp package -DskipTests，检查依赖与自动配置资源；条件允许时启动 Example 手工验证 HTTP。
