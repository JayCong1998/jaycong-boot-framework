# 商城 React 迁移计划

**目标：** 使用已确认的 React Router + Zustand 方案替换 Vue，保持页面与业务细节。

- [x] 替换 Vite 插件、依赖和 TSX 入口；保留代理、素材、CSS 和服务层。
- [x] 将认证、购物车、提示迁移至 Zustand，保持存储键、数量限制和异常语义。
- [x] 将布局、首页、轮播、认证表单、购物车转换为 React；Radix Dialog 保持模态焦点与键盘操作。
- [x] 更新 README，运行 npm run typecheck、npm run build，启动页面手工检查。

遵循项目约定，不创建或修改测试代码。不提交已有未提交改动。

验证限制：商品接口当前不可用；手机视口验证未完成，不将其记为已通过。
