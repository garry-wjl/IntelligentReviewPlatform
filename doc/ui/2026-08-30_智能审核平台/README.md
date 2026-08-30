# 智能审核平台 UI

对应 PRD：`doc/产品方案/2026-08-30_智能审核平台-PRD.md`

本期仅 PC 端。目录：

- `web/`：Ant Design Pro（Vite + React + TypeScript + antd + `@ant-design/pro-components`）

## 配色

- 品牌主色：Ant Design 默认蓝 `#1677FF`
- 主题：default（未启用 compact / dark）

## 启动

```bash
cd web
npm install
npm run dev
```

浏览器打开终端提示的本地地址（默认 `http://localhost:5173`）。

数据全部 Mock，保存在浏览器 `localStorage`（键 `review-scoring-mock-v1`）。接入设置页可「重置演示数据」。
