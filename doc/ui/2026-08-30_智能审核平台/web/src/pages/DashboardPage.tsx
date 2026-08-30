import { PageContainer, ProCard, StatisticCard } from '@ant-design/pro-components';
import { Button, List, Space, Tag, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '../mock/store';
import { TaskStatusTag } from '../components/StatusTags';

export default function DashboardPage() {
  const state = useAppStore();
  const navigate = useNavigate();
  const published = state.ruleSets.filter((r) => r.isCurrent).length;
  const liveTasks = state.tasks.filter((t) => !t.isTrial);
  const pending = liveTasks.filter((t) => t.status === 'type_pending').length;
  const failedHooks = state.webhookLogs.filter((w) => w.status === 'failed').length;

  return (
    <PageContainer content="智能审核平台分三块：审核规则（标准）、审核器管理（怎么评）、审核任务（评什么）。数据保存在浏览器本地。">
      <StatisticCard.Group>
        <StatisticCard statistic={{ title: '规则集', value: state.types.length }} />
        <StatisticCard statistic={{ title: '审核器', value: state.auditors.length }} />
        <StatisticCard statistic={{ title: '已发布版本', value: published }} />
        <StatisticCard statistic={{ title: '审核任务', value: liveTasks.length }} />
        <StatisticCard statistic={{ title: '待确认规则集', value: pending }} />
        <StatisticCard statistic={{ title: 'Webhook 失败', value: failedHooks }} />
      </StatisticCard.Group>
      <ProCard title="建议走一遍的演示路径" style={{ marginTop: 16 }} extra={<Button type="primary" onClick={() => navigate('/tasks/create')}>创建审核任务</Button>}>
        <List
          dataSource={[
            '审核规则 → 新建规则集（系统生成编号）→ 定义规则与评估分方式 → 试评 / 发布',
            '审核器管理 → 新建 Agent 审核器并选择 Agent 平台中的 Agent，或新建普通审核器',
            '审核任务 → 指定规则集 + 审核器 + 多附件，观察解析→打分→scored',
            '不指定规则集且材料含「未知」→ 待确认规则集 → 指定后重评',
            '对 scored 任务改分、标注、锁定；同一业务单号验证幂等',
          ]}
          renderItem={(item, i) => (
            <List.Item>
              <Space>
                <Tag color="blue">{i + 1}</Tag>
                <Typography.Text>{item}</Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      </ProCard>
      <ProCard title="最近任务" style={{ marginTop: 16 }} extra={<Button type="link" onClick={() => navigate('/tasks')}>全部</Button>}>
        <List
          dataSource={state.tasks.slice(0, 6)}
          renderItem={(item) => (
            <List.Item onClick={() => navigate(`/tasks/${item.id}`)} style={{ cursor: 'pointer' }}>
              <List.Item.Meta
                title={
                  <Space>
                    {item.bizId}
                    {item.isTrial ? <Tag>试评</Tag> : null}
                    <TaskStatusTag status={item.status} />
                  </Space>
                }
                description={`${item.typeName ?? '未定规则集'} · ${item.auditorName ?? '未指定审核器'} · ${item.createdAt}`}
              />
            </List.Item>
          )}
        />
      </ProCard>
    </PageContainer>
  );
}
