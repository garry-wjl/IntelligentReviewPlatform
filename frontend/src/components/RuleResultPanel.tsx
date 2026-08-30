import { Alert, Descriptions, Empty, Space, Statistic, Table, Tag, Typography } from 'antd';
import type { EvaluationTask } from '../types/models';
import { displayScore, PASS_MODE_LABEL, rulePassed } from '../utils/ruleDisplay';
import { PassTag } from './StatusTags';

export default function RuleResultPanel({ task }: { task: EvaluationTask }) {
  if (!task.results.length) {
    return <Empty description={task.status === 'type_pending' ? '等待指定规则集后再评，不会给出分数' : '暂无逐条结果'} />;
  }

  const passedText = task.passed == null ? '未出结果' : task.passed ? '通过' : '未通过';
  const totalText =
    task.totalScore != null
      ? String(task.totalScore)
      : task.passMode === 'all_pass'
        ? '不汇总（全部通过）'
        : '—';

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Space size={48} wrap>
        <Statistic
          title="是否通过"
          value={passedText}
          valueStyle={{ color: task.passed ? '#3f8600' : task.passed === false ? '#cf1322' : undefined }}
        />
        <Statistic title="总得分" value={totalText} />
      </Space>
      <Descriptions size="small" bordered column={3}>
        <Descriptions.Item label="评估分方式">{task.passMode ? PASS_MODE_LABEL[task.passMode] : '-'}</Descriptions.Item>
        <Descriptions.Item label="总得分">{task.totalScore ?? '（全部通过模式不汇总总分）'}</Descriptions.Item>
        <Descriptions.Item label="结果">
          <PassTag passed={task.passed} complete={task.complete} />
        </Descriptions.Item>
      </Descriptions>
      {!task.complete ? (
        <Alert type="warning" showIcon message="存在机评失败的规则，结果标记为不完整。全部通过或红线条失败时整单不得判通过。" />
      ) : null}
      <Table
        rowKey="ruleId"
        size="small"
        pagination={false}
        dataSource={task.results}
        expandable={{
          expandedRowRender: (row) => (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Typography.Paragraph style={{ marginBottom: 8 }}>{row.machineRationale || row.failReason || '暂无理由'}</Typography.Paragraph>
              {row.humanReason ? <Typography.Text type="secondary">改分原因：{row.humanReason}</Typography.Text> : null}
              {row.evidence.map((ev, i) => (
                <Alert
                  key={i}
                  type={ev.locationFound ? 'info' : 'warning'}
                  message={`${ev.fileName}${ev.location ? ` · ${ev.location}` : ''}${ev.locationFound ? '' : ' · 位置未定位'}`}
                  description={ev.quote}
                />
              ))}
            </Space>
          ),
        }}
        columns={[
          { title: '规则', dataIndex: 'ruleName' },
          {
            title: '区间',
            render: (_, r) => `${r.minScore} ~ ${r.maxScore}`,
            width: 100,
          },
          { title: '通过分', dataIndex: 'passScore', width: 80 },
          {
            title: '机评',
            width: 80,
            render: (_, r) => (r.failed ? <Tag color="error">失败</Tag> : r.machineScore),
          },
          {
            title: '人评',
            width: 80,
            render: (_, r) => r.humanScore ?? '—',
          },
          {
            title: '展示分',
            width: 80,
            render: (_, r) => displayScore(r) ?? '—',
          },
          {
            title: '该条',
            width: 90,
            render: (_, r) => {
              const ok = rulePassed(r);
              if (ok == null) return <Tag>无分</Tag>;
              return <Tag color={ok ? 'success' : 'error'}>{ok ? '通过' : '未通过'}</Tag>;
            },
          },
          {
            title: '红线',
            width: 70,
            render: (_, r) => (r.isVeto ? <Tag color="red">红线</Tag> : '—'),
          },
          {
            title: '判分原因',
            ellipsis: true,
            render: (_, r) => r.machineRationale || r.failReason || '—',
          },
        ]}
      />
    </Space>
  );
}
