import { useEffect, useMemo, useState } from 'react';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { Alert, Button, Form, Input, Select, Space, Steps, Tag, Typography, message } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { mimeOf, toTaskDetail } from '../../api/adapters';
import { createTrial, getEvaluationDetail, pollEvaluationUntilSettled } from '../../api/evaluation';
import { getRuleSetDetail } from '../../api/ruleset';
import RuleResultPanel from '../../components/RuleResultPanel';
import { TaskStatusTag } from '../../components/StatusTags';
import type { DocType, EvaluationTask, Scene } from '../../types/models';
import { pageBreadcrumb } from '../../utils/breadcrumb';
import { PASS_MODE_LABEL } from '../../utils/ruleDisplay';
import { loadEnabledRuleSets, loadEnabledScenes } from '../../utils/lookups';

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
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form] = Form.useForm();
  const sceneNum = Form.useWatch('sceneNum', form) as string | undefined;
  const typeCode = Form.useWatch('typeCode', form) as string | undefined;
  const [scenes, setScenes] = useState<Scene[]>([]);
  const [ruleSets, setRuleSets] = useState<DocType[]>([]);
  const [task, setTask] = useState<EvaluationTask>();
  const [running, setRunning] = useState(false);

  const selectedScene = scenes.find((item) => item.id === sceneNum);
  const selectedRuleSet = ruleSets.find((item) => item.code === typeCode);
  const sceneRuleSets = useMemo(
    () =>
      ruleSets.filter(
        (item) => item.sceneNum === sceneNum && item.enabled && item.currentVersionNo != null,
      ),
    [ruleSets, sceneNum],
  );
  const extraParams = (selectedScene?.params ?? []).filter((item) => !item.builtin);
  const wizardStep = task ? 3 : typeCode ? 2 : sceneNum ? 1 : 0;

  const hydrateTask = async (num: string) => {
    const detail = await getEvaluationDetail(num);
    let ruleSetName: string | undefined;
    if (detail.ruleSetNum) {
      try {
        ruleSetName = (await getRuleSetDetail(detail.ruleSetNum)).name;
      } catch {
        ruleSetName = detail.ruleSetNum;
      }
    }
    setTask(toTaskDetail(detail, { ruleSetName }));
  };

  useEffect(() => {
    void Promise.all([loadEnabledScenes(), loadEnabledRuleSets()]).then(([sceneList, sets]) => {
      setScenes(sceneList);
      setRuleSets(sets);
    });
  }, []);

  useEffect(() => {
    const taskId = params.get('taskId');
    if (taskId) void hydrateTask(taskId).catch((e: Error) => message.error(e.message));
  }, [params]);

  const create = async () => {
    const values = await form.validateFields();
    const attachments = ((values.attachments as { fileName?: string; role?: string; fileUrl?: string }[]) ?? [])
      .filter((item) => item.fileName?.trim());
    if (!attachments.length) {
      message.warning('请至少填写一份 Attachment');
      return;
    }
    const extras: Record<string, string> = {};
    extraParams.forEach((item) => {
      const value = (values.extra as Record<string, string> | undefined)?.[item.key];
      if (value != null && String(value).trim()) extras[item.key] = String(value);
    });
    setRunning(true);
    try {
      const created = await createTrial({
        bizId: (values.bizId as string) || `TASK-${Date.now()}`,
        ruleSetNum: values.typeCode as string,
        inputText: values.inputText as string | undefined,
        extraParams: extras,
        attachments: attachments.map((item) => ({
          fileName: item.fileName!.trim(),
          role: item.role || 'main',
          mime: mimeOf(item.fileName!.trim()),
          fileUrl: item.fileUrl,
        })),
      });
      message.success(created.idempotent ? '命中幂等：返回已有任务' : '已开始审核，正在轮询结果');
      const detail = await pollEvaluationUntilSettled(created.num);
      let ruleSetName: string | undefined;
      if (detail.ruleSetNum) {
        try {
          ruleSetName = (await getRuleSetDetail(detail.ruleSetNum)).name;
        } catch {
          ruleSetName = detail.ruleSetNum;
        }
      }
      setTask(toTaskDetail(detail, { ruleSetName }));
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const currentStep = task ? STEP_INDEX[task.status] ?? 0 : 0;

  return (
    <PageContainer
      title="创建审核任务"
      breadcrumb={pageBreadcrumb([
        { title: '审核任务', path: '/tasks' },
        { title: '创建审核任务' },
      ])}
      content="先选场景，再选该场景下已发布的规则集，然后按场景参数填写内容并开跑。审核器已经绑在规则上，这里不用再选。管理端创建的任务不发 Webhook。"
    >
      <Steps
        size="small"
        current={wizardStep}
        style={{ marginBottom: 16 }}
        items={[{ title: '选择场景' }, { title: '选择规则集' }, { title: '填写参数' }, { title: '查看结果' }]}
      />
      <ProCard split="vertical" headerBordered>
        <ProCard title="创建任务" colSpan="40%">
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              bizId: `TASK-${Date.now()}`,
              attachments: [{ fileName: '', role: 'main', fileUrl: '' }],
            }}
          >
            <Form.Item name="sceneNum" label="场景" rules={[{ required: true, message: '请先选择场景' }]}>
              <Select
                placeholder="请选择场景"
                options={scenes.map((item) => ({
                  value: item.id,
                  label: `${item.name}（${item.id}）`,
                }))}
                onChange={() => {
                  form.setFieldsValue({ typeCode: undefined });
                  setTask(undefined);
                }}
              />
            </Form.Item>
            <Form.Item
              name="typeCode"
              label="规则集"
              rules={[{ required: true, message: '请选择规则集' }]}
              extra={
                selectedRuleSet?.passMode
                  ? `评估分方式：${PASS_MODE_LABEL[selectedRuleSet.passMode]}${
                      selectedRuleSet.passMode !== 'all_pass'
                        ? `，总分通过线 ${selectedRuleSet.overallPassScore ?? '—'}`
                        : ''
                    }`
                  : undefined
              }
            >
              <Select
                placeholder={sceneNum ? '请选择已发布规则集' : '请先选择场景'}
                disabled={!sceneNum}
                options={sceneRuleSets.map((item) => ({
                  value: item.code,
                  label: `${item.name}（${item.code} · v${item.currentVersionNo}）`,
                }))}
                onChange={() => setTask(undefined)}
              />
            </Form.Item>
            {sceneNum && !sceneRuleSets.length ? (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message="该场景下没有已启用且已发布的规则集"
              />
            ) : null}

            {selectedScene ? (
              <>
                <Typography.Title level={5}>场景参数</Typography.Title>
                <Form.Item name="inputText" label="Input（用户输入）">
                  <Input.TextArea rows={4} placeholder="不限长度的用户输入内容" />
                </Form.Item>
                {extraParams.map((item) => (
                  <Form.Item key={item.key} name={['extra', item.key]} label={`${item.label}（${item.key}）`}>
                    <Input placeholder={`请输入 ${item.label}`} />
                  </Form.Item>
                ))}
                <Form.List name="attachments">
                  {(fields, { add, remove }) => (
                    <>
                      <Typography.Text>Attachment（附件）</Typography.Text>
                      {fields.map((field) => (
                        <Space key={field.key} align="baseline" style={{ display: 'flex', marginTop: 8 }} wrap>
                          <Form.Item {...field} name={[field.name, 'fileName']}>
                            <Input style={{ width: 180 }} placeholder="文件名" />
                          </Form.Item>
                          <Form.Item {...field} name={[field.name, 'role']} initialValue="main">
                            <Select
                              style={{ width: 120 }}
                              options={[
                                { value: 'main', label: 'main 正文' },
                                { value: 'appendix', label: 'appendix' },
                                { value: 'financial', label: 'financial' },
                              ]}
                            />
                          </Form.Item>
                          <Form.Item {...field} name={[field.name, 'fileUrl']}>
                            <Input style={{ width: 180 }} placeholder="可选 URL" />
                          </Form.Item>
                          {fields.length > 1 ? <MinusCircleOutlined onClick={() => remove(field.name)} /> : null}
                        </Space>
                      ))}
                      <Button
                        type="dashed"
                        onClick={() => add({ fileName: '', role: 'appendix', fileUrl: '' })}
                        block
                        icon={<PlusOutlined />}
                        style={{ marginTop: 8, marginBottom: 16 }}
                      >
                        添加附件
                      </Button>
                    </>
                  )}
                </Form.List>
              </>
            ) : (
              <Alert type="info" showIcon style={{ marginBottom: 16 }} message="选择场景后，会按该场景展开 Input、Attachment 和扩展参数。" />
            )}

            <Form.Item name="bizId" label="业务单号" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Space wrap>
              <Button type="primary" onClick={() => void create()} loading={running} disabled={!typeCode}>
                开始审核
              </Button>
              <Button onClick={() => task && navigate(`/tasks/${task.id}`)} disabled={!task}>
                打开任务详情
              </Button>
            </Space>
          </Form>
        </ProCard>
        <ProCard title="审核结果">
          {task ? (
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Space wrap>
                <Typography.Text strong>{task.bizId}</Typography.Text>
                <TaskStatusTag status={task.status} />
                <Tag>{task.id}</Tag>
                {task.typeName || task.typeCode ? <Tag color="blue">{task.typeName ?? task.typeCode}</Tag> : null}
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
                  { title: '完成' },
                  { title: '锁定' },
                ]}
              />
              <RuleResultPanel task={task} />
              <Alert type="info" showIcon message="管理端不提供改分、标注与锁定。请使用开放 API。" />
            </Space>
          ) : (
            <Alert message="按左侧顺序选场景、选规则集、填写参数后开跑。结果会给出是否通过、总得分，以及每条规则的得分和判分原因。" />
          )}
        </ProCard>
      </ProCard>
    </PageContainer>
  );
}
