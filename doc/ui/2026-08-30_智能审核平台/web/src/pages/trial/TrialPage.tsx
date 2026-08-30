import { useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { Alert, Button, Form, Select, Space, Steps, Upload, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { api, useAppStore } from '../../mock/store';
import RuleResultPanel from '../../components/RuleResultPanel';
import { TaskStatusTag } from '../../components/StatusTags';

export default function TrialPage() {
  const { typeId, ruleSetId } = useParams();
  const state = useAppStore();
  const navigate = useNavigate();
  const docType = state.types.find((t) => t.id === typeId);
  const ruleSet = state.ruleSets.find((r) => r.id === ruleSetId);
  const [taskId, setTaskId] = useState<string>();
  const [files, setFiles] = useState<{ name: string; role: string }[]>([]);
  const [auditorId, setAuditorId] = useState('aud-agent-quality');
  const task = state.tasks.find((t) => t.id === taskId);
  const running = task && ['received', 'parsing', 'classifying', 'scoring'].includes(task.status);

  if (!docType || !ruleSet) return <PageContainer title="规则集不存在" />;

  const start = () => {
    if (!files.length) {
      message.warning('请先添加样例文件');
      return;
    }
    try {
      const { task: created } = api.createEvaluation({
        bizId: `trial-${Date.now()}`,
        typeCode: docType.code,
        auditorId,
        files,
        isTrial: true,
        ruleSetId: ruleSet.id,
      });
      setTaskId(created.id);
      message.success('已开始试评，不会向接入方发送 Webhook');
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  return (
    <PageContainer
      title={`试评 · ${docType.name}`}
      subTitle={ruleSet.version ? `v${ruleSet.version}` : '当前草稿'}
      extra={
        <Button onClick={() => navigate(`/rule-sets/${docType.id}/versions/${ruleSet.id}`)}>返回规则集</Button>
      }
    >
      <Alert
        showIcon
        type="info"
        style={{ marginBottom: 16 }}
        message="试评会写入任务列表并标记为「试评」，不产生对外 Webhook。可用文件名触发分支：含「缺风险」降低红线分；含「损坏」模拟解析失败。"
      />
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
        <span>审核器</span>
        <Select
          style={{ width: 260 }}
          value={auditorId}
          onChange={setAuditorId}
          options={state.auditors
            .filter((a) => a.enabled)
            .map((a) => ({
              value: a.id,
              label: `${a.name}（${a.kind === 'agent' ? 'Agent' : '普通'}）`,
            }))}
        />
        <Button type="primary" loading={!!running} onClick={start}>
          开始试评
        </Button>
        <Button
          onClick={() =>
            setFiles([
              { name: '立项报告-缺风险.pdf', role: 'main' },
              { name: '测算表.xlsx', role: 'financial' },
            ])
          }
        >
          填入缺风险样例
        </Button>
        <Button onClick={() => setFiles([{ name: '立项报告-完整版.pdf', role: 'main' }])}>填入完整样例</Button>
        {task ? <TaskStatusTag status={task.status} /> : null}
        {task ? <span>进度 {task.results.length}/{ruleSet.rules.length}</span> : null}
      </Space>
      {running ? (
        <Steps
          size="small"
          current={['received', 'parsing', 'classifying', 'scoring', 'scored'].indexOf(task!.status)}
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
