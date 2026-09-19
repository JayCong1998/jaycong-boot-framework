# Maven 工程初始化计划

**Goal:** 建立用户已确认的首版模块，只初始化 Maven 工程。

**Architecture:** 根 POM 聚合模块并管理构建插件，独立 BOM 管理框架与第三方版本。Core 不依赖 Spring，示例依赖 Web 和 MyBatis Starter。

**Tech Stack:** Java 21、Maven 3.9+、Spring Boot 3.5.16、MyBatis-Plus 3.5.17。

**Spec:** 用户已确认 BOM、Core、Context、Web Starter、MyBatis Starter、Example 六个模块，本次限定为 Maven 初始化。

## 执行步骤

- [x] 创建独立 BOM，导入 Spring Boot BOM，管理框架模块和 MyBatis-Plus 版本。
- [x] 创建聚合 POM，导入框架 BOM，固定编译、测试、资源、打包和安装插件版本。
- [x] 创建五个 Java 模块 POM 与标准源码目录，声明模块依赖；示例引入 H2 和 MySQL runtime 驱动。
- [x] 添加 README、编辑器配置、忽略规则；明确当前没有应用启动类。
- [x] 执行 `mvn -B -ntp clean verify`，检查全部七个 reactor 项目成功，并检查 Core 依赖树。

## 验证边界

本次没有业务实现，不添加占位测试。验证 Maven 模型、依赖解析和打包流程；应用启动与功能测试属于后续实现。
