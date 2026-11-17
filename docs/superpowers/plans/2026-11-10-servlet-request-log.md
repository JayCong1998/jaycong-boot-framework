# Servlet 请求日志实施清单

目标：common-web 自动注册 Filter，记录请求开始和结束，包含 MVC 失败响应。

- [x] 新增 RequestLogFilter：requestId、URL、参数、状态码、耗时、有限长度请求和响应内容；JSON 敏感字段脱敏，文件和流式内容跳过。
- [x] 自动配置注册可覆盖、可关闭的 Filter，更新 docs/web.md。
- [x] 执行 mvn -B -ntp -pl :jaycong-auth -am package -DskipTests，并启动临时端口手工验证成功、校验失败和 404。

遵循 AGENTS.md，不新增或修改测试代码，不提交工作区已有改动。未处理异常在 Filter 处记录并抛出，容器之后生成的错误页不采集。

验证结果：构建成功；临时端口 18094 验证 200、DTO 校验失败 400、畸形 JSON 400 和 404，均有开始/结束及响应内容；查询密码和 JSON 账号/密码已脱敏。验证进程已停止。异步和未处理异常路径未做 HTTP 实测。
