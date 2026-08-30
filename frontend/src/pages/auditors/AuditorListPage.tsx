import { useEffect, useRef, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Radio, Select, Space, Switch, Tag, message } from 'antd';
import { toAuditor, UI_TO_KIND } from '../../api/adapters';
import { createAuditor, pageAuditors, setAuditorEnabled, syncAgents, updateAuditor } from '../../api/auditor';
import type { AgentOption, Auditor, AuditorKind } from '../../types/models';
import { loadAgents } from '../../utils/lookups';
import { keywordSearchColumn } from '../../utils/search';

export default function AuditorListPage() {
  const actionRef = useRef<ActionType>();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Auditor | null>(null);
  const [agents, setAgents] = useState<AgentOption[]>([]);
  const [form] = Form.useForm();
  const kind = Form.useWatch('kind', form);

  useEffect(() => {
    void loadAgents()
      .then(setAgents)
      .catch((e: Error) => message.error(e.message));
  }, []);

  const submit = async () => {
    const values = await form.validateFields();
    try {
      const kind = values.kind as AuditorKind;
      const payload = {
        name: values.name as string,
        kind: UI_TO_KIND[kind],
        agentNum: kind === 'agent' ? (values.agentId as string | undefined) : undefined,
        description: values.description as string | undefined,
      };
      if (editing) {
        await updateAuditor({ num: editing.id, ...payload });
        message.success('已保存');
      } else {
        await createAuditor(payload);
        message.success('已创建审核器');
      }
      setOpen(false);
      actionRef.current?.reload();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const columns: ProColumns<Auditor>[] = [
    keywordSearchColumn<Auditor>(),
    { title: '编号', dataIndex: 'id', width: 140, search: false },
    { title: '名称', dataIndex: 'name', search: false },
    {
      title: '类型',
      width: 140,
      search: false,
      render: (_, a) => (a.kind === 'agent' ? <Tag color="blue">Agent 审核器</Tag> : <Tag>普通审核器</Tag>),
    },
    { title: '绑定 Agent', search: false, render: (_, a) => a.agentName ?? '—' },
    { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
    {
      title: '启用',
      width: 90,
      search: false,
      render: (_, a) => (
        <Switch
          checked={a.enabled}
          onChange={async (v) => {
            try {
              await setAuditorEnabled({ num: a.id, enabled: v });
              actionRef.current?.reload();
            } catch (e) {
              message.error((e as Error).message);
            }
          }}
        />
      ),
    },
    {
      title: '操作',
      width: 100,
      search: false,
      render: (_, a) => (
        <Button
          type="link"
          onClick={() => {
            setEditing(a);
            form.setFieldsValue(a);
            setOpen(true);
          }}
        >
          编辑
        </Button>
      ),
    },
  ];

  return (
    <PageContainer
      content="审核器决定任务如何执行打分。跑任务时选择一个已启用的审核器。Agent 审核器绑定 Agent 平台上的 Agent；普通审核器不调用大模型。"
      extra={
        <Space>
          <Button
            onClick={async () => {
              try {
                await syncAgents();
                const next = await loadAgents();
                setAgents(next);
                message.success('已同步 Agent 目录');
              } catch (e) {
                message.error((e as Error).message);
              }
            }}
          >
            同步 Agent
          </Button>
          <Button
            type="primary"
            onClick={() => {
              setEditing(null);
              form.resetFields();
              form.setFieldsValue({ kind: 'agent' });
              setOpen(true);
            }}
          >
            新建审核器
          </Button>
        </Space>
      }
    >
      <ProTable<Auditor>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        options={false}
        search={{ labelWidth: 'auto' }}
        pagination={{ pageSize: 10 }}
        request={async (params) => {
          const page = await pageAuditors({
            pageNo: params.current,
            pageSize: params.pageSize,
            keyword: params.keyword as string | undefined,
          });
          return { data: (page.list ?? []).map(toAuditor), success: true, total: page.total ?? 0 };
        }}
        locale={{ emptyText: '尚无审核器。请先创建。' }}
      />
      <Modal title={editing ? '编辑审核器' : '新建审核器'} open={open} onOk={submit} onCancel={() => setOpen(false)} destroyOnHidden>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="kind" label="审核器类型" rules={[{ required: true }]}>
            <Radio.Group>
              <Radio value="agent">Agent 审核器</Radio>
              <Radio value="ordinary">普通审核器</Radio>
            </Radio.Group>
          </Form.Item>
          {kind === 'agent' ? (
            <Form.Item name="agentId" label="选择 Agent（来自 Agent 平台）" rules={[{ required: true, message: '请选择 Agent' }]}>
              <Select
                options={agents.map((ag) => ({
                  value: ag.id,
                  label: ag.name,
                  ag,
                }))}
                optionRender={(option) => (
                  <Space direction="vertical" size={0}>
                    <span>{option.data.ag.name}</span>
                    <span style={{ color: '#999', fontSize: 12 }}>{option.data.ag.description}</span>
                  </Space>
                )}
              />
            </Form.Item>
          ) : (
            <Form.Item>
              <span style={{ color: '#666' }}>普通审核器不绑定 Agent，任务执行时不发起语义调用。</span>
            </Form.Item>
          )}
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
