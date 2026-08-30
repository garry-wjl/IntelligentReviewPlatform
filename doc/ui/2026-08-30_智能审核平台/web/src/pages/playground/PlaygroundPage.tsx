import { useMemo, useState } from 'react';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Divider,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Steps,
  Switch,
  Tag,
  Typography,
  message,
} from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, useAppStore } from '../../mock/store';
import RuleResultPanel from '../../components/RuleResultPanel';
import { TaskStatusTag } from '../../components/StatusTags';
import { displayScore } from '../../mock/engine';

const STEP_INDEX: Record<string, number> = {
  received: 0,
  parsing: 1,
  classifying: 2,
  type_pending: 2,
  scoring: 3,
  scored: 4,
  finalized: 5,
  failed: 1,
};

export default function PlaygroundPage() {
  const state = useAppStore();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [taskId, setTaskId] = useState(params.get('taskId') || 'EVL-demo-scored');
  const [form] = Form.useForm();
  const specifyRuleSet = Form.useWatch('specifyRuleSet', form);
  const [patchRuleId, setPatchRuleId] = useState<string>();
  const [newScore, setNewScore] = useState<number | null>(null);
  const [reason, setReason] = useState('');
  const [ann, setAnn] = useState('');
  const task = state.tasks.find((t) => t.id === taskId);
  const running = task && ['received', 'parsing', 'classifying', 'scoring'].includes(task.status);
  const enabledRuleSets = state.types.filter((t) => t.enabled);
  const enabledAuditors = state.auditors.filter((a) => a.enabled);

  const create = async (preset?: Record<string, unknown>) => {
    const values = preset ?? (await form.validateFields());
    try {
      const files = String(values.files)
        .split('\n')
        .map((line: string) => line.trim())
        .filter(Boolean)
        .map((line: string) => {
          const [name, role] = line.split('|').map((s) => s.trim());
          return { name, role };
        });
      const { task: created, idempotent } = api.createEvaluation({
        bizId: values.bizId as string,
        typeCode: values.specifyRuleSet ? (values.typeCode as string) : undefined,
        auditorId: values.auditorId as string,
        files,
      });
      setTaskId(created.id);
      message.success(idempotent ? '命中幂等：返回已有任务，未重新机评' : '已创建任务，异步审核中');
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const result = task?.results.find((r) => !r.failed) ?? task?.results[0];
  const currentStep = task ? STEP_INDEX[task.status] ?? 0 : 0;

  const presets = useMemo(
    () => [
      {
        label: '指定规则集 · Agent 审核器 · 缺风险',
        values: {
          bizId: `APP-${Date.now()}`,
          specifyRuleSet: true,
          typeCode: 'RS-0001',
          auditorId: 'aud-agent-quality',
          files: '立项报告-缺风险.pdf | main\n测算表.xlsx | financial',
        },
      },
      {
        label: '指定规则集 · 普通审核器',
        values: {
          bizId: `ORD-${Date.now()}`,
          specifyRuleSet: true,
          typeCode: 'RS-0001',
          auditorId: 'aud-ordinary',
          files: '立项报告-完整版.pdf | main',
        },
      },
      {
        label: '不指定规则集 · 自动匹配立项',
        values: {
          bizId: `APP-${Date.now()}`,
          specifyRuleSet: false,
          typeCode: undefined,
          auditorId: 'aud-agent-quality',
          files: '立项可行性报告.pdf | main',
        },
      },
      {
        label: '低置信度 → 待确认规则集',
        values: {
          bizId: `MAIL-${Date.now()}`,
          specifyRuleSet: false,
          auditorId: 'aud-agent-quality',
          files: '未知材料.pdf | main',
        },
      },
      {
        label: '季度报告 · 无已发布版本',
        values: {
          bizId: `Q-${Date.now()}`,
          specifyRuleSet: true,
          typeCode: 'RS-0002',
          auditorId: 'aud-agent-quality',
          files: '三季度工作报告.docx | main',
        },
      },
      {
        label: '全部附件损坏 → 解析失败',
        values: {
          bizId: `BAD-${Date.now()}`,
          specifyRuleSet: true,
          typeCode: 'RS-0001',
          auditorId: 'aud-agent-quality',
          files: '损坏文件.pdf | main',
        },
      },
      {
        label: '末条 Agent 超时（不完整）',
        values: {
          bizId: `TO-${Date.now()}`,
          specifyRuleSet: true,
          typeCode: 'RS-0001',
          auditorId: 'aud-agent-quality',
          files: '立项报告-超时.pdf | main',
        },
      },
    ],
    [],
  );

  return (
    <PageContainer content="创建审核任务时选择规则集与审核器。规则集决定评什么标准；审核器决定怎么评。也可不指定规则集，由系统自动匹配。">
      <ProCard split="vertical" headerBordered>
        <ProCard title="创建审核任务" colSpan="38%">
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              bizId: 'APP-NEW-001',
              specifyRuleSet: true,
              typeCode: 'RS-0001',
              auditorId: 'aud-agent-quality',
              files: '立项报告-缺风险.pdf | main\n测算表.xlsx | financial',
            }}
          >
            <Form.Item name="bizId" label="业务单号（幂等键）" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="auditorId" label="审核器" rules={[{ required: true, message: '请选择审核器' }]}>
              <Select
                options={enabledAuditors.map((a) => ({
                  value: a.id,
                  label: `${a.name}（${a.kind === 'agent' ? 'Agent' : '普通'}）`,
                }))}
              />
            </Form.Item>
            <Form.Item name="specifyRuleSet" label="指定规则集" valuePropName="checked">
              <Switch checkedChildren="指定" unCheckedChildren="自动匹配" />
            </Form.Item>
            {specifyRuleSet ? (
              <Form.Item name="typeCode" label="规则集编号">
                <Select options={enabledRuleSets.map((t) => ({ value: t.code, label: `${t.code} ${t.name}` }))} />
              </Form.Item>
            ) : null}
            <Form.Item name="files" label="附件（每行：文件名 | role）" rules={[{ required: true }]}>
              <Input.TextArea rows={4} />
            </Form.Item>
            <Space wrap>
              <Button type="primary" onClick={() => create()} loading={!!running}>
                创建 / 幂等重放
              </Button>
              <Button onClick={() => task && navigate(`/tasks/${task.id}`)}>打开任务详情</Button>
            </Space>
          </Form>
          <Divider />
          <Typography.Text type="secondary">快捷场景</Typography.Text>
          <Space direction="vertical" style={{ width: '100%', marginTop: 8 }}>
            {presets.map((p) => (
              <Button
                key={p.label}
                block
                onClick={() => {
                  form.setFieldsValue(p.values);
                  void create(p.values);
                }}
              >
                {p.label}
              </Button>
            ))}
          </Space>
        </ProCard>
        <ProCard title="任务运行时">
          {task ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Space wrap>
                <Typography.Text strong>{task.bizId}</Typography.Text>
                <TaskStatusTag status={task.status} />
                <Tag>{task.id}</Tag>
                {task.auditorName ? <Tag color="geekblue">{task.auditorName}</Tag> : null}
              </Space>
              <Steps
                size="small"
                current={currentStep}
                status={task.status === 'failed' ? 'error' : task.status === 'type_pending' ? 'wait' : undefined}
                items={[
                  { title: '接收' },
                  { title: '解析' },
                  { title: '匹配规则集' },
                  { title: '逐条审核' },
                  { title: 'scored' },
                  { title: 'finalized' },
                ]}
              />
              <RuleResultPanel task={task} />
              {task.status === 'scored' ? (
                <ProCard size="small" title="模拟改分 / 标注 / 锁定" headerBordered>
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Space wrap>
                      <span>规则</span>
                      <Select
                        style={{ minWidth: 220 }}
                        value={patchRuleId ?? result?.ruleId}
                        options={task.results.filter((r) => !r.failed).map((r) => ({ value: r.ruleId, label: r.ruleName }))}
                        onChange={setPatchRuleId}
                      />
                      <InputNumber
                        placeholder="新分数"
                        min={result?.minScore}
                        max={result?.maxScore}
                        value={newScore}
                        onChange={setNewScore}
                      />
                      <Input
                        placeholder="改分原因"
                        style={{ width: 220 }}
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                      />
                      <Button
                        onClick={() => {
                          try {
                            const ruleId = patchRuleId || result?.ruleId;
                            if (!ruleId || newScore == null) {
                              message.warning('请选择规则并填写分数');
                              return;
                            }
                            api.patchScore(task.id, ruleId, Number(newScore), reason);
                            message.success('已改分并即时重算');
                          } catch (e) {
                            message.error((e as Error).message);
                          }
                        }}
                      >
                        提交改分
                      </Button>
                    </Space>
                    <Space>
                      <Input
                        placeholder="标注内容"
                        style={{ width: 320 }}
                        value={ann}
                        onChange={(e) => setAnn(e.target.value)}
                      />
                      <Button
                        onClick={() => {
                          try {
                            api.addAnnotation(task.id, {
                              target: 'file',
                              fileId: task.attachments[0]?.id,
                              location: '模拟位置',
                              content: ann,
                            });
                            message.success('已加标注');
                          } catch (e) {
                            message.error((e as Error).message);
                          }
                        }}
                      >
                        添加文档标注
                      </Button>
                      <Button
                        type="primary"
                        onClick={() => {
                          try {
                            api.finalize(task.id);
                            message.success('已锁定');
                          } catch (e) {
                            message.error((e as Error).message);
                          }
                        }}
                      >
                        锁定终态
                      </Button>
                    </Space>
                    {result ? (
                      <Typography.Text type="secondary">
                        当前展示分 {displayScore(result) ?? '-'}，区间 {result.minScore}~{result.maxScore}
                      </Typography.Text>
                    ) : null}
                  </Space>
                </ProCard>
              ) : null}
              {task.status === 'type_pending' || task.status === 'scored' ? (
                <Space>
                  <span>指定规则集并重评</span>
                  <Select
                    style={{ width: 240 }}
                    placeholder="选择规则集"
                    options={enabledRuleSets.map((t) => ({ value: t.code, label: `${t.code} ${t.name}` }))}
                    onChange={(code) => {
                      try {
                        api.reclassify(task.id, code);
                        message.success('已重评，人工分将被覆盖');
                      } catch (e) {
                        message.error((e as Error).message);
                      }
                    }}
                  />
                </Space>
              ) : null}
              {task.status === 'finalized' ? <Alert type="success" showIcon message="已锁定，改分 / 标注 / 重评均会拒绝。" /> : null}
            </Space>
          ) : (
            <Alert message="请创建任务或从任务详情跳转" />
          )}
        </ProCard>
      </ProCard>
    </PageContainer>
  );
}
