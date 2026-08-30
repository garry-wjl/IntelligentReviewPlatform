import { useMemo, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { api, useAppStore } from '../../mock/store';
import { PASS_MODE_HINT, PASS_MODE_LABEL, ruleCompleteness, validateRuleSet } from '../../mock/engine';
import type { PassMode, Rule } from '../../types/models';
import { RuleSetStatusTag } from '../../components/StatusTags';

const emptyRule = (): Rule => ({
  id: `rule-${Date.now()}`,
  name: '',
  standard: '',
  minScore: 0,
  maxScore: 10,
  passScore: 6,
  weight: 1,
  isVeto: false,
  positiveExample: '',
  negativeExample: '',
  sort: 1,
});

export default function RuleSetEditPage() {
  const { typeId, ruleSetId } = useParams();
  const state = useAppStore();
  const navigate = useNavigate();
  const docType = state.types.find((t) => t.id === typeId);
  const ruleSet = state.ruleSets.find((r) => r.id === ruleSetId);
  const readonly = ruleSet?.status !== 'draft';
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [current, setCurrent] = useState<Rule | null>(null);
  const [form] = Form.useForm<Rule>();

  const errors = useMemo(() => (ruleSet ? validateRuleSet(ruleSet) : []), [ruleSet]);

  if (!docType || !ruleSet) return <PageContainer title="规则集不存在" />;

  const saveMeta = (passMode: PassMode, overallPassScore: number) => {
    try {
      api.updateRuleSet(ruleSet.id, { passMode, overallPassScore });
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const openRule = (rule?: Rule) => {
    const next = rule ?? emptyRule();
    setCurrent(next);
    form.setFieldsValue(next);
    setDrawerOpen(true);
  };

  const saveRule = async () => {
    const values = await form.validateFields();
    try {
      api.upsertRule(ruleSet.id, { ...(current as Rule), ...values });
      message.success('规则已保存');
      setDrawerOpen(false);
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const publish = () => {
    Modal.confirm({
      title: '发布规则集',
      content: `将生成新版本，进行中的旧任务仍使用其创建时的版本。发布后新任务使用本版本。`,
      onOk: () => {
        try {
          const version = api.publish(ruleSet.id);
          message.success(`已发布为 v${version}`);
        } catch (e) {
          message.error((e as Error).message);
          return Promise.reject();
        }
      },
    });
  };

  return (
    <PageContainer
      title={`${docType.name} / ${ruleSet.version ? `v${ruleSet.version}` : '规则集草稿'}`}
      subTitle={
        <Space>
          <RuleSetStatusTag status={ruleSet.status} isCurrent={ruleSet.isCurrent} />
          {ruleSet.basedOnVersion ? `基于 v${ruleSet.basedOnVersion} 另存` : null}
        </Space>
      }
      extra={[
        <Button key="trial" onClick={() => navigate(`/rule-sets/${docType.id}/versions/${ruleSet.id}/trial`)}>
          试评
        </Button>,
        readonly ? (
          <Button
            key="copy"
            type="primary"
            onClick={() => {
              const draft = api.createDraft(docType.id, ruleSet.id);
              navigate(`/rule-sets/${docType.id}/versions/${draft.id}`);
            }}
          >
            复制为新草稿
          </Button>
        ) : (
          <Button key="pub" type="primary" disabled={errors.length > 0} onClick={publish}>
            发布
          </Button>
        ),
      ]}
    >
      {readonly ? <Alert type="info" showIcon style={{ marginBottom: 16 }} message="已发布版本只读。线上任务绑定创建时的版本，修改请复制为新草稿。" /> : null}
      {!readonly && errors.length ? (
        <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="发布前请处理" description={errors.map((e) => e).join('；')} />
      ) : null}

      <Typography.Title level={5}>评估分方式</Typography.Title>
      <Radio.Group
        disabled={readonly}
        value={ruleSet.passMode}
        onChange={(e) => {
          Modal.confirm({
            title: '切换评估分方式',
            content: '「全部通过」不使用权重/红线；「加权总分」需要权重；「红线 + 加权」需要红线条且红线不计入加权。',
            onOk: () => saveMeta(e.target.value, ruleSet.overallPassScore),
          });
        }}
      >
        {(Object.keys(PASS_MODE_LABEL) as PassMode[]).map((k) => (
          <Radio key={k} value={k}>
            {PASS_MODE_LABEL[k]}
          </Radio>
        ))}
      </Radio.Group>
      <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>
        {PASS_MODE_HINT[ruleSet.passMode]}
      </Typography.Paragraph>
      {ruleSet.passMode !== 'all_pass' ? (
        <Space style={{ marginBottom: 16 }}>
          <span>总分通过线</span>
          <InputNumber
            disabled={readonly}
            min={0}
            value={ruleSet.overallPassScore}
            onChange={(v) => saveMeta(ruleSet.passMode, Number(v || 0))}
          />
        </Space>
      ) : null}

      <Space style={{ marginBottom: 12 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          规则清单
        </Typography.Title>
        {!readonly ? (
          <Button type="dashed" onClick={() => openRule()}>
            添加规则
          </Button>
        ) : null}
      </Space>
      <Table
        rowKey="id"
        size="small"
        pagination={false}
        dataSource={[...ruleSet.rules].sort((a, b) => a.sort - b.sort)}
        columns={[
          { title: '序', dataIndex: 'sort', width: 50 },
          { title: '名称', dataIndex: 'name' },
          { title: '区间', render: (_, r) => `${r.minScore}-${r.maxScore}`, width: 90 },
          { title: '通过分', dataIndex: 'passScore', width: 80 },
          { title: '权重', dataIndex: 'weight', width: 70 },
          { title: '红线', width: 70, render: (_, r) => (r.isVeto ? <Tag color="red">是</Tag> : '否') },
          { title: '状态', width: 100, render: (_, r) => <Tag>{ruleCompleteness(r)}</Tag> },
          {
            title: '操作',
            width: 220,
            render: (_, r) => (
              <Space>
                <Button type="link" onClick={() => openRule(r)}>
                  {readonly ? '查看' : '编辑'}
                </Button>
                {!readonly ? (
                  <>
                    <Button type="link" onClick={() => api.moveRule(ruleSet.id, r.id, -1)}>
                      上移
                    </Button>
                    <Button type="link" onClick={() => api.moveRule(ruleSet.id, r.id, 1)}>
                      下移
                    </Button>
                    <Popconfirm title="删除该规则？" onConfirm={() => api.removeRule(ruleSet.id, r.id)}>
                      <Button type="link" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  </>
                ) : null}
              </Space>
            ),
          },
        ]}
      />

      <Drawer
        title={current ? `规则 · ${current.name || '未命名'}` : '规则'}
        width={560}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          readonly ? null : (
            <Button type="primary" onClick={saveRule}>
              保存
            </Button>
          )
        }
      >
        <Form form={form} layout="vertical" disabled={readonly}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="standard"
            label="评审标准（写给人看的标准，不用写提示词）"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={5} />
          </Form.Item>
          <Space>
            <Form.Item name="minScore" label="最低分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="maxScore" label="最高分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="passScore" label="通过分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="weight" label="权重">
              <InputNumber min={0} />
            </Form.Item>
          </Space>
          <Form.Item name="isVeto" label="红线" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="positiveExample" label="正例（什么样算高分，选填）">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="negativeExample" label="反例（什么样算低分，选填）">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Drawer>
    </PageContainer>
  );
}
