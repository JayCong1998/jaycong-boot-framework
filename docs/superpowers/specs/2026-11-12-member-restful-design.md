# 会员管理接口与数据刷新优化

用户已确认：详情和编辑均读取最新接口数据；资料与状态更新统一 PUT；检查管理端所有 HTTP 请求并遵守 RESTful 语义。

详情使用 GET /admin/members/{id}，编辑打开时也重新读取该接口，获取昵称与 lockVersion。弹窗加载期间不允许提交，读取失败显示重试；关闭、切换对象和组件卸载时使旧请求失效。详情支持手动刷新和失败重试。写入成功后刷新列表和已打开详情，防止旧读取覆盖写入结果。

前后端同步采用 PUT /admin/members/{id} 和 PUT /admin/members/{id}/status，保持现有参数、权限、脱敏和乐观锁。登录 POST、查询 GET；管理端退出改用 DELETE /auth/session，Auth 保留 POST /auth/logout 兼容商城。不存在的新增或删除会员功能不创建空接口。

不修改测试代码，以 npm run build 和相关 Maven 模块 package -DskipTests 验证。更新接口文档，说明旧 PATCH 调用需要随服务一起升级。
