import { useEffect, useState } from 'react';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Alert, Button, Descriptions, Empty, List, Space, Tag, Timeline, Typography, message } from 'antd';
import { useParams } from 'react-router-dom';
import { pageWebhooks } from '../../api/access';
import { toTaskDetail, toWebhookLog } from '../../api/adapters';
import { getAuditorDetail } from '../../api/auditor';
import { getAttachmentUrl, getEvaluationDetail } from '../../api/evaluation';
import { getRuleSetDetail } from '../../api/ruleset';
import RuleResultPanel from '../../components/RuleResultPanel';
import { PassTag, TaskStatusTag, TypeSourceTag } from '../../components/StatusTags';
import type { EvaluationTask, WebhookLog } from '../../types/models';
import { auditorDisplay, isTaskLevelAuditor } from '../../utils/lookups';
import { pageBreadcrumb } from '../../utils/breadcrumb';
import { PASS_MODE_LABEL } from '../../utils/ruleDisplay';

export default function TaskDetailPage() {
  const { taskId } = useParams();
  const [task, setTask] = useState<EvaluationTask>();
  const [hooks, setHooks] = useState<WebhookLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void (async () => {
      if (!taskId) return;
      setLoading(true);
      try {
        const detail = await getEvaluationDetail(taskId);
        let ruleSetName: string | undefined;
        let auditorName: string | undefined;
        if (detail.ruleSetNum) {
          try {
            ruleSetName = (await getRuleSetDetail(detail.ruleSetNum)).name;
          } catch {
            ruleSetName = detail.ruleSetNum;
          }
        }
        if (isTaskLevelAuditor(detail.auditorNum)) {
          try {
            auditorName = (await getAuditorDetail(detail.auditorNum)).name;
          } catch {
            auditorName = detail.auditorNum;
          }
        }
        setTask(toTaskDetail(detail, { ruleSetName, auditorName }));
        if (!detail.trial) {
          const page = await pageWebhooks({ pageNo: 1, pageSize: 50, evaluationNum: taskId });
          setHooks((page.list ?? []).map(toWebhookLog));
        } else {
          setHooks([]);
        }
      } catch (e) {
        message.error((e as Error).message);
        setTask(undefined);
      } finally {
        setLoading(false);
      }
    })();
  }, [taskId]);

  const crumbs = pageBreadcrumb([
    { title: '审核任务', path: '/tasks' },
    { title: task?.bizId || (loading ? '任务详情' : '任务不存在') },
  ]);

  if (!loading && !task) return <PageContainer title="任务不存在" breadcrumb={crumbs} />;
  if (!task) return <PageContainer loading breadcrumb={crumbs} />;

  const openAttachment = async (fileNum: string) => {
    try {
      const result = await getAttachmentUrl(task.id, fileNum);
      if (result.url) window.open(result.url, '_blank');
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  return (
    <PageContainer
      title={task.bizId}
      subTitle={task.id}
      breadcrumb={crumbs}
      extra={<TaskStatusTag status={task.status} />}
      content={
        <Descriptions size="small" column={3}>
          <Descriptions.Item label="种类">{task.isTrial ? '试评（无 Webhook）' : '审核任务'}</Descriptions.Item>
          <Descriptions.Item label="规则集">
            <Space>
              {task.typeName ?? task.typeCode ?? '未定'}
              {task.typeCode ? <Tag>{task.typeCode}</Tag> : null}
              <TypeSourceTag source={task.typeSource} />
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="版本">{task.ruleSetVersion ? `v${task.ruleSetVersion}` : task.isTrial ? '草稿快照' : '—'}</Descriptions.Item>
          <Descriptions.Item label="审核器">
            {auditorDisplay(task)}
            {task.auditorKind === 'agent' ? ` · Agent${task.agentName ? `（${task.agentName}）` : ''}` : task.auditorKind === 'ordinary' ? ' · 普通' : ''}
          </Descriptions.Item>
          <Descriptions.Item label="评估分方式">{task.passMode ? PASS_MODE_LABEL[task.passMode] : '—'}</Descriptions.Item>
          <Descriptions.Item label="汇总">
            <PassTag passed={task.passed} complete={task.complete} />
            {task.totalScore != null ? <span> · 总分 {task.totalScore}</span> : null}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">{task.updatedAt}</Descriptions.Item>
        </Descriptions>
      }
    >
      <Alert
        showIcon
        type="info"
        style={{ marginBottom: 16 }}
        message="本页只读，用于排查与审计。改分 / 标注 / 锁定仅开放 API 提供。"
      />
      {task.status === 'type_pending' ? (
        <Alert showIcon type="warning" style={{ marginBottom: 16 }} message="等待指定规则集" description={task.classifyReason} />
      ) : null}
      {task.status === 'failed' ? (
        <Alert showIcon type="error" style={{ marginBottom: 16 }} message="任务失败" description={task.failReason} />
      ) : null}
      {task.classifyConfidence != null ? (
        <Alert
          showIcon
          style={{ marginBottom: 16 }}
          type={task.status === 'type_pending' ? 'warning' : 'info'}
          message={`识别置信度 ${Math.round(task.classifyConfidence * 100)}%`}
          description={task.classifyReason}
        />
      ) : null}

      <ProCard split="vertical" headerBordered>
        <ProCard title="内容包" colSpan="32%">
          {task.attachments.length ? (
            <List
              dataSource={task.attachments}
              renderItem={(f) => (
                <List.Item
                  actions={[
                    <Button key="dl" type="link" onClick={() => void openAttachment(f.id)}>
                      下载
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        {f.name}
                        <Tag>{f.role}</Tag>
                        {f.parseFailed ? <Tag color="error">解析失败</Tag> : null}
                      </Space>
                    }
                    description={
                      <Typography.Paragraph ellipsis={{ rows: 3 }} type="secondary">
                        {f.excerpt}
                      </Typography.Paragraph>
                    }
                  />
                </List.Item>
              )}
            />
          ) : (
            <Empty />
          )}
        </ProCard>
        <ProCard title="逐条结果">
          <RuleResultPanel task={task} />
        </ProCard>
      </ProCard>

      <ProCard title="标注" style={{ marginTop: 16 }}>
        {task.annotations.length ? (
          <List
            dataSource={task.annotations}
            renderItem={(a) => (
              <List.Item>
                <List.Item.Meta
                  title={`${a.target === 'file' ? '文档标注' : '规则标注'} · ${a.actor || '接入方'} · ${a.createdAt}`}
                  description={`${a.location ?? ''} ${a.content}`}
                />
              </List.Item>
            )}
          />
        ) : (
          <Empty description="无人标注" />
        )}
      </ProCard>

      <ProCard title="操作时间线" style={{ marginTop: 16 }} split="vertical">
        <ProCard colSpan="60%">
          <Timeline
            items={task.timeline.map((e) => ({
              children: (
                <div>
                  <div>
                    {e.at} · {e.actor} · {e.title}
                  </div>
                  {e.detail ? <Typography.Text type="secondary">{e.detail}</Typography.Text> : null}
                </div>
              ),
            }))}
          />
        </ProCard>
        <ProCard title="Webhook">
          {task.isTrial ? (
            <Empty description="试评不发送 Webhook" />
          ) : hooks.length ? (
            <List
              dataSource={hooks}
              renderItem={(w) => (
                <List.Item>
                  <Space>
                    <Tag color={w.status === 'success' ? 'success' : w.status === 'failed' ? 'error' : 'default'}>{w.status}</Tag>
                    {w.event}
                    <Typography.Text type="secondary">{w.at}</Typography.Text>
                  </Space>
                </List.Item>
              )}
            />
          ) : (
            <Empty description="暂无投递记录" />
          )}
        </ProCard>
      </ProCard>
    </PageContainer>
  );
}
