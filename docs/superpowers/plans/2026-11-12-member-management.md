# 会员管理实施计划

用户已确认第一期方案并授权直接完成前后端，不增加操作审计或测试代码。

- [x] Auth 新增管理员专用会员分页、详情、昵称修改和状态修改接口；仅管理 role=user，联系方式脱敏，更新检查 lockVersion，禁用撤销会话。
- [x] Gateway 和 Vite 增加 /admin/members 路由代理。
- [x] mall-admin 用真实会员页替换占位页，提供筛选、分页、详情抽屉、编辑和状态确认。
- [x] Maven package -DskipTests、前端 typecheck/build；用 HTTP 验证授权、查询、更新及禁用，打包占用与浏览器验证限制见 docs/members.md。
- [x] 更新接口和部署说明，记录实际验证及限制。

接口使用统一 Result 和 PageResult(total, rows)，分页参数 page/size，会员 ID 为字符串。无建表变更；保留现有审计字段自动填充，但不新增审计日志功能。订单、积分、删除、密码和联系方式变更不属于第一期。
