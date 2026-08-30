import { useEffect, useState } from 'react';
import { PageContainer, ProCard, StatisticCard } from '@ant-design/pro-components';
import { Button, List, Space, Tag, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { pageWebhooks } from '../api/access';
import { toTask } from '../api/adapters';
import { pageAuditors } from '../api/auditor';
import { pageEvaluations } from '../api/evaluation';
import { pageRuleSets } from '../api/ruleset';
import { TaskStatusTag } from '../components/StatusTags';
import type { EvaluationTask } from '../types/models';
import { auditorDisplay } from '../utils/lookups';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [ruleSetCount, setRuleSetCount] = useState(0);
  const [auditorCount, setAuditorCount] = useState(0);
  const [published, setPublished] = useState(0);
  const [liveCount, setLiveCount] = useState(0);
  const [pending, setPending] = useState(0);
  const [failedHooks, setFailedHooks] = useState(0);
  const [recent, setRecent] = useState<EvaluationTask[]>([]);

  useEffect(() => {
    void (async () => {
      try {
        const [ruleSets, auditors, tasks, pendingPage, hooks, latest] = await Promise.all([
          pageRuleSets({ pageNo: 1, pageSize: 200 }),
          pageAuditors({ pageNo: 1, pageSize: 1 }),
          pageEvaluations({ pageNo: 1, pageSize: 1, isTrial: false }),
          pageEvaluations({ pageNo: 1, pageSize: 1, status: 'TYPE_PENDING' }),
          pageWebhooks({ pageNo: 1, pageSize: 1, status: 'failed' }),
          pageEvaluations({ pageNo: 1, pageSize: 6 }),
        ]);
        setRuleSetCount(ruleSets.total ?? 0);
        setPublished((ruleSets.list ?? []).filter((r) => r.currentVersionNo != null).length);
        setAuditorCount(auditors.total ?? 0);
        setLiveCount(tasks.total ?? 0);
        setPending(pendingPage.total ?? 0);
        setFailedHooks(hooks.total ?? 0);
        setRecent((latest.list ?? []).map((item) => toTask(item)));
      } catch {
        /* 后端未启动时仍展示空工作台 */
      }
    })();
  }, []);

  return (
    <PageContainer content="智能审核平台：先建场景，再写审核规则，再用审核器跑审核任务。数据来自后端 HTTP 接口。">
      <StatisticCard.Group>
        <StatisticCard statistic={{ title: '规则集', value: ruleSetCount }} />
        <StatisticCard statistic={{ title: '审核器', value: auditorCount }} />
        <StatisticCard statistic={{ title: '已发布版本', value: published }} />
        <StatisticCard statistic={{ title: '审核任务', value: liveCount }} />
        <StatisticCard statistic={{ title: '待确认规则集', value: pending }} />
        <StatisticCard statistic={{ title: 'Webhook 失败', value: failedHooks }} />
      </StatisticCard.Group>
      <ProCard title="建议走一遍的演示路径" style={{ marginTop: 16 }} extra={<Button type="primary" onClick={() => navigate('/tasks/create')}>创建审核任务</Button>}>
        <List
          dataSource={[
            '场景管理 → 新建场景（内置 Input / Attachment，可加扩展参数）',
            '审核规则 → 选择场景创建规则集 → 详情页维护草稿版本规则 → 试评 / 发布',
            '审核器管理 → 在规则上绑定 Agent 审核器或普通审核器',
            '审核任务 → 选场景 → 选规则集 → 填写场景参数 → 跑出是否通过、总分与逐条得分',
            '开放 API 创建的审核任务会走 Webhook；管理端创建不发 Webhook',
            '改分、标注、锁定仅开放 API 提供，管理端只读排查',
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
          dataSource={recent}
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
                description={`${item.typeName ?? item.typeCode ?? '未定规则集'} · ${auditorDisplay(item)} · ${item.createdAt}`}
              />
            </List.Item>
          )}
        />
      </ProCard>
    </PageContainer>
  );
}
