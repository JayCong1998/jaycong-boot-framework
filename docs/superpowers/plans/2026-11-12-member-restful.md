# 会员管理 RESTful 优化实施计划

**Goal:** 详情和编辑读取最新会员数据，管理端请求按 HTTP 语义调用。

**Architecture:** 保持 Vue service 与 User Controller 分层及现有乐观锁；用请求序号隔离过期响应，Auth 增加当前会话 DELETE 入口。

**Tech Stack:** Vue 3、TypeScript、Axios、Spring Boot、Maven。

- [x] 修改 mall-admin/src/services/members.ts 与 MemberController.java：`http.put` 对应 `@PutMapping`，请求体保持 nickname/status 与 lockVersion。
- [x] 修改 MembersView.vue：详情和编辑独立加载、错误及请求序号；编辑调用 detail，关闭和卸载使序号失效；提交成功后重读已打开详情。
- [x] 修改 mall-admin/src/services/auth.ts 和 AuthController.java：`http.delete('/auth/session')` 对应 `@DeleteMapping("/session")`，保留原退出入口兼容商城。
- [x] 更新 docs/members.md、docs/auth.md、mall-admin/README.md，记录方法语义与升级要求。
- [x] 执行 `npm run build`（mall-admin）及 `mvn -B -ntp -pl jaycong-business/jaycong-user,jaycong-auth -am package -DskipTests -Dspring-boot.repackage.skip=true`，检查实际输出，不新增或修改测试代码。

## 验证结果

2026-11-12：mall-admin 中 `npm run build` 通过（包含 vue-tsc）；相关后端模块 Maven package 通过，命令按计划执行并跳过测试与 Boot repackage。未启动新版服务或进行浏览器/HTTP 联调，运行中的旧服务不会自动加载本次代码；部署时须同步更新前端、User 和 Auth。
