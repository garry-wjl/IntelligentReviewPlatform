import { useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Radio, Select, Space, Switch, Tag, message } from 'antd';
import { api, useAppStore } from '../../mock/store';
import type { Auditor } from '../../types/models';
import { textIncludes } from '../../utils/search';

export default function AuditorListPage() {
  const state = useAppStore();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Auditor | null>(null);
  const [form] = Form.useForm();
  const kind = Form.useWatch('kind', form);

  const submit = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        api.updateAuditor(editing.id, values);
        message.success('已保存');
      } else {
        api.createAuditor(values);
        message.success('已创建审核器');
      }
      setOpen(false);
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  return (
    <PageContainer
      content="审核器决定任务如何执行打分。跑任务时选择一个已启用的审核器。Agent 审核器绑定 Agent 平台上的 Agent；普通审核器不调用大模型，按材料可解析性给出基线分后再按规则集评估方式汇总。"
      extra={
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
      }
    >
      <ProTable<Auditor>
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        options={false}
        pagination={{ pageSize: 10 }}
        params={{ stamp: state.auditors.map((a) => a.id).join(',') }}
        request={async (params) => {
          const list = state.auditors.filter(
            (a) => textIncludes(a.id, params.id as string) && textIncludes(a.name, params.name as string),
          );
          return { data: list, success: true, total: list.length };
        }}
        columns={[
          { title: '编号', dataIndex: 'id', width: 140, fieldProps: { placeholder: '支持模糊搜索' } },
          { title: '名称', dataIndex: 'name', fieldProps: { placeholder: '支持模糊搜索' } },
          {
            title: '类型',
            width: 140,
            search: false,
            render: (_, a) =>
              a.kind === 'agent' ? <Tag color="blue">Agent 审核器</Tag> : <Tag>普通审核器</Tag>,
          },
          { title: '绑定 Agent', search: false, render: (_, a) => a.agentName ?? '—' },
          { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
          {
            title: '启用',
            width: 90,
            search: false,
            render: (_, a) => (
              <Switch checked={a.enabled} onChange={(v) => api.setAuditorEnabled(a.id, v)} />
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
        ]}
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
                options={state.agents.map((ag) => ({
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
