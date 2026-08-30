# 智能审核平台 · Web

对应 PRD：`doc/产品方案/2026-08-30_智能审核平台-PRD.md`

技术栈：Vite + React 18 + TypeScript + **Ant Design 5** + **@ant-design/pro-components**（ProLayout / PageContainer / ProTable / ProCard / StatisticCard）。

主色：`#1677FF`（Ant Design 默认品牌蓝），`ConfigProvider` 中文语言包。

## 脚本

```bash
npm install
npm run dev
npm run build
```

## 页面与 PRD 功能对应

| 路由 | 覆盖 |
|---|---|
| `/` 工作台 | 概览与演示路径 |
| `/types` | F1 类型 CRUD、编码创建后不可改、停用 |
| `/types/:id` | F2 规则集版本、复制草稿、停用当前发布 |
| `/types/:id/rulesets/:id` | F3 F4 F6 自然语言规则、三种通过模式、排序、发布 |
| `/types/:id/rulesets/:id/trial` | F5 多附件试评，无 Webhook |
| `/tasks` `/tasks/:id` | F23 F24 只读排查；F25 后台不改分 |
| `/playground` | F13–F19 创建、幂等、识别/待确认、改分、标注、锁定、重评 |
| `/settings` | F20 F21 凭证、回调、事件订阅、死信重试 |

Mock 引擎还覆盖：多附件解析部分失败、全部解析失败、无可用规则、单条 Agent 超时（不完整结果）、规则版本冻结。
