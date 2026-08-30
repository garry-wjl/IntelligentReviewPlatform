import { Tag } from 'antd';
import type { RuleSetStatus, TaskStatus, TypeSource } from '../types/models';

export const TASK_STATUS_META: Record<TaskStatus, { color: string; text: string }> = {
  received: { color: 'default', text: '已接收' },
  parsing: { color: 'processing', text: '解析中' },
  classifying: { color: 'processing', text: '识别中' },
  scoring: { color: 'processing', text: '机评中' },
  type_pending: { color: 'warning', text: '待确认规则集' },
  scored: { color: 'blue', text: '机评完成' },
  finalized: { color: 'success', text: '已锁定' },
  failed: { color: 'error', text: '失败' },
};

export function TaskStatusTag({ status }: { status: TaskStatus }) {
  const meta = TASK_STATUS_META[status] ?? { color: 'default', text: status };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

export function RuleSetStatusTag({ status, isCurrent }: { status: RuleSetStatus; isCurrent?: boolean }) {
  if (status === 'draft') return <Tag>草稿</Tag>;
  if (status === 'published' && isCurrent) return <Tag color="success">当前发布</Tag>;
  if (status === 'published') return <Tag color="blue">已发布</Tag>;
  if (status === 'archived') return <Tag>历史版本</Tag>;
  return <Tag color="error">已停用</Tag>;
}

export function PassTag({ passed, complete }: { passed?: boolean; complete?: boolean }) {
  if (passed == null) return <Tag>未出结果</Tag>;
  return (
    <>
      <Tag color={passed ? 'success' : 'error'}>{passed ? '整单通过' : '整单不通过'}</Tag>
      {complete === false ? <Tag color="warning">不完整</Tag> : null}
    </>
  );
}

export function TypeSourceTag({ source }: { source?: TypeSource }) {
  if (!source) return <Tag>未定</Tag>;
  return <Tag color={source === 'specified' ? 'geekblue' : 'purple'}>{source === 'specified' ? '指定规则集' : '自动匹配'}</Tag>;
}
