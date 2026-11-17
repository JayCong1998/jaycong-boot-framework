---
name: ddd-module-design
description: Use when designing, implementing, or reviewing Java/Spring business modules using lightweight DDD 分包 and 分层, Facade boundaries, entity behavior, and direct Mapper persistence, including reuse across projects. Does not impose this style on projects with a different chosen architecture.
---

# DDD模块设计

用于在不同 Java/Spring 工程中设计和实现轻量 DDD 业务模块。先按业务能力划分模块，再确定入口、流程编排、领域行为和技术实现的归属。使用目标工程的业务语言、依赖和约定。

## 阅读顺序

先读 [架构规范](references/architecture-rules.md)。需要理解名词时查 [术语表](references/glossary.md)，其余按任务选择：

| 任务 | 说明书章节 |
|---|---|
| 选择模块结构、简单服务或复杂编排 | [业务模块组织方式](references/module-patterns.md) |
| 从需求落到类和方法 | [功能实现步骤](references/adaptation-guide.md) |
| 通过一个模块理解接口、实体、SQL、流水及失败处理 | [设备借用审批案例](references/complete-example.md) |
| 审查实现、确认边界与测试范围 | [验收清单](references/review-checklist.md) |

只保留设备借用审批这一个模块案例，用于说明职责和调用链。案例中的包名、状态、字段与业务假设不属于目标工程的要求；按实际需求替换。技能内容自包含，无需访问其他工程源码。

## 核心约定

- 目标工程指用户正在开发的工程。业务名、根包、模块名、状态和错误码全部使用目标工程的业务语言，不复制说明书示例的名称。
- 公开契约放在独立 API 模块或 `api/<业务>`；HTTP 输入放 `param`；`facade` 实现公开契约；`controller` 处理 HTTP 适配及可信身份。
- 简单流程由 Facade 调用领域服务；需要跨业务或多步骤协调时增加 `application`。领域服务允许直接使用 `infrastructure/mapper`，实体允许兼作 ORM 映射对象，不默认增加 Repository 和独立 DO。
- 有生命周期的实体通过业务方法校验和改变状态。状态机、Manage/Read 分离、工厂与模板服务按复杂度选用。
- 外部协议和供应商 SDK 放在技术适配实现中；跨业务调用公开契约。实体不发 HTTP、不发消息、不调用 Mapper。
- 写操作明确权限、业务校验、幂等键、并发控制、本地事务及失败语义。异常需要越过事务边界或明确触发回滚，再转换为对外失败响应。
- 缓存、消息、分片、分布式锁和分布式事务由需求决定；本地数据库事务不自动覆盖远程调用或异步执行。

先检查目标工程已有约定及用户要求。若已选择纯领域、独立 DO 或 Repository，则保留该选择，在其约束下使用相同的职责划分。

## 实现与交付

按变更复杂度说明新增包、关键类、调用链及必要的失败处理，然后实施。既有小改动只说明相关部分。验证实际业务行为，并如实报告执行的检查及未验证范围。

需要长期项目指令时，可采用 [AI 入口片段](assets/AGENTS.fragment.md)。正文中的目录和代码均为组织示例；生成实现时替换为目标工程已有依赖和业务规则。
