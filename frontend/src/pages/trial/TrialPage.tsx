import { useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { Alert, Button, Form, Input, Select, Space, Steps, Upload, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { mimeOf, toDocType, toRuleSet, toTaskDetail } from '../../api/adapters';
import { createTrial, pollEvaluationUntilSettled } from '../../api/evaluation';
import { getRuleSetDetail, getRuleSetVersion } from '../../api/ruleset';
import RuleResultPanel from '../../components/RuleResultPanel';
import { TaskStatusTag } from '../../components/StatusTags';
import type { DocType, EvaluationTask, RuleSet } from '../../types/models';
import { pageBreadcrumb } from '../../utils/breadcrumb';

const STEP_KEYS = ['received', 'parsing', 'classifying', 'scoring', 'scored'];

export default function TrialPage() {
  const { typeId, ruleSetId } = useParams();
  const navigate = useNavigate();
  const [docType, setDocType] = useState<DocType>();
  const [ruleSet, setRuleSet] = useState<RuleSet>();
  const [task, setTask] = useState<EvaluationTask>();
  const [files, setFiles] = useState<{ name: string; role: string; url?: string }[]>([]);
  const [inputText, setInputText] = useState('');
  const [extraValues, setExtraValues] = useState<Record<string, string>>({});
  const [running, setRunning] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    void (async () => {
      if (!typeId || !ruleSetId) return;
      try {
        const [detail, version] = await Promise.all([getRuleSetDetail(typeId), getRuleSetVersion(ruleSetId)]);
        setDocType(toDocType(detail));
        setRuleSet(toRuleSet(version));
      } catch (e) {
        message.error((e as Error).message);
      } finally {
        setReady(true);
      }
    })();
  }, [typeId, ruleSetId]);

  const detailPath = typeId ? `/rule-sets/${typeId}${ruleSetId ? `?version=${ruleSetId}` : ''}` : '/rule-sets';
  const crumbs = pageBreadcrumb([
    { title: '审核规则', path: '/rule-sets' },
    { title: docType?.name || '规则集详情', path: typeId ? `/rule-sets/${typeId}` : undefined },
    { title: '试评' },
  ]);

  if (!ready) return <PageContainer loading breadcrumb={crumbs} />;
  if (!docType || !ruleSet) return <PageContainer title="规则集不存在" breadcrumb={crumbs} />;

  const start = async () => {
    if (!files.length) {
      message.warning('请先添加样例文件');
      return;
    }
    setRunning(true);
    try {
      const extras = Object.fromEntries(
        Object.entries(extraValues).filter(([, value]) => value.trim()),
      );
      const created = await createTrial({
        bizId: `trial-${Date.now()}`,
        ruleSetNum: docType.id,
        ruleSetVersionNum: ruleSet.id,
        inputText,
        extraParams: extras,
        attachments: files.map((f) => ({
          fileName: f.name,
          role: f.role,
          mime: mimeOf(f.name),
          fileUrl: f.url,
        })),
      });
      message.success('已开始试评，将轮询任务状态');
      const detail = await pollEvaluationUntilSettled(created.num);
      setTask(toTaskDetail(detail, { ruleSetName: docType.name }));
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  return (
    <PageContainer
      title={`试评 · ${docType.name}`}
      subTitle={ruleSet.version ? `v${ruleSet.version}` : '当前草稿'}
      breadcrumb={crumbs}
      extra={<Button onClick={() => navigate(detailPath)}>返回规则集</Button>}
    >
      <Alert
        showIcon
        type="info"
        style={{ marginBottom: 16 }}
        message="试评会写入任务列表并标记为「试评」，不产生对外 Webhook。每条规则使用其绑定的审核器打分。请按场景参数填写内容后开跑。"
      />
      <Form.Item label="Input（用户输入）" style={{ marginBottom: 12 }}>
        <Input.TextArea
          rows={4}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="不限长度的用户输入内容"
        />
      </Form.Item>
      {(docType.params ?? [])
        .filter((item) => !item.builtin)
        .map((item) => (
          <Form.Item key={item.key} label={`${item.label}（${item.key}）`} style={{ marginBottom: 12 }}>
            <Input
              value={extraValues[item.key] ?? ''}
              onChange={(e) => setExtraValues((prev) => ({ ...prev, [item.key]: e.target.value }))}
              placeholder={`请输入 ${item.label}`}
            />
          </Form.Item>
        ))}
      <Upload.Dragger
        multiple
        beforeUpload={(file) => {
          setFiles((prev) => [...prev, { name: file.name, role: prev.length ? 'appendix' : 'main' }]);
          return false;
        }}
        fileList={files.map((f, i) => ({ uid: String(i), name: `${f.name}（${f.role}）` }))}
        onRemove={(file) => {
          setFiles((prev) => prev.filter((_, i) => String(i) !== file.uid));
        }}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽添加样例内容包（多附件）</p>
      </Upload.Dragger>
      <Form layout="inline" style={{ marginTop: 12 }}>
        {files.map((f, i) => (
          <Form.Item key={i} label={f.name}>
            <Select
              style={{ width: 140 }}
              value={f.role}
              options={[
                { value: 'main', label: 'main 正文' },
                { value: 'appendix', label: 'appendix' },
                { value: 'financial', label: 'financial' },
              ]}
              onChange={(role) => setFiles((prev) => prev.map((x, idx) => (idx === i ? { ...x, role } : x)))}
            />
          </Form.Item>
        ))}
      </Form>
      <Space style={{ marginTop: 12, marginBottom: 16 }}>
        <Button type="primary" loading={running} onClick={() => void start()}>
          开始试评
        </Button>
        {task ? <TaskStatusTag status={task.status} /> : null}
        {task ? (
          <span>
            进度 {task.results.length}/{ruleSet.rules.length}
          </span>
        ) : null}
      </Space>
      {running || (task && ['received', 'parsing', 'classifying', 'scoring'].includes(task.status)) ? (
        <Steps
          size="small"
          current={STEP_KEYS.indexOf(task?.status ?? 'received')}
          items={[{ title: '接收' }, { title: '解析' }, { title: '识别' }, { title: '逐条机评' }, { title: '完成' }]}
          style={{ marginBottom: 16 }}
        />
      ) : null}
      {task ? <RuleResultPanel task={task} /> : null}
      {task ? (
        <Button type="link" onClick={() => navigate(`/tasks/${task.id}`)}>
          在任务详情中查看
        </Button>
      ) : null}
    </PageContainer>
  );
}
