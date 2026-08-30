# 智能审核平台 · 管理端

独立生产前端，只通过 HTTP 调用后端 `/admin/v1`，不内嵌原型里的 Mock 打分引擎。

技术栈：Vite + React 18 + TypeScript + Ant Design 5 + `@ant-design/pro-components`（ProLayout / PageContainer / ProTable / ProCard）。HashRouter，主色 `#1677FF`。

## 启动

依赖后端已在 `http://localhost:8080` 启动。

```bash
cd frontend
npm i
npm run dev
```

开发期 Vite 把 `/admin/v1`、`/open/v1` 代理到 `http://localhost:8080`。

## 环境变量

| 变量 | 说明 | 默认 |
|---|---|---|
| `VITE_API_BASE` | 管理端 API 前缀 | `/admin/v1`（见 `.env.development`） |

请求头：`Authorization: Bearer ${localStorage.token || 'dev-sso'}`。

统一响应：`{code,msg,data,rows}`，`code === 200` 为成功。

## 构建

```bash
npm run build
npm run preview
```
