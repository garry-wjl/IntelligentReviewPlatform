import { useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Space, Switch, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { api, useAppStore } from '../../mock/store';
import type { DocType } from '../../types/models';
import { textIncludes } from '../../utils/search';

export default function TypeListPage() {
  const state = useAppStore();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<DocType | null>(null);
  const [form] = Form.useForm();

  const submit = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        api.updateType(editing.id, values);
        message.success('已保存');
      } else {
        const created = api.createType(values);
        message.success(`已创建，系统编号 ${created.code}`);
        setOpen(false);
        navigate(`/rule-sets/${created.id}`);
        return;
      }
      setOpen(false);
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  return (
    <PageContainer
      content="创建规则集时只需填写名称，编号由系统生成。进入后可定义规则与评估分方式。"
      extra={
        <Button
          type="primary"
          onClick={() => {
            setEditing(null);
            form.resetFields();
            setOpen(true);
          }}
        >
          新建规则集
        </Button>
      }
    >
      <ProTable<DocType>
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        options={false}
        pagination={{ pageSize: 10 }}
        params={{ stamp: state.types.map((t) => t.id).join(',') }}
        request={async (params) => {
          const list = state.types.filter(
            (t) => textIncludes(t.code, params.code as string) && textIncludes(t.name, params.name as string),
          );
          return { data: list, success: true, total: list.length };
        }}
        columns={[
          { title: '编号', dataIndex: 'code', width: 140, fieldProps: { placeholder: '支持模糊搜索' } },
          { title: '名称', dataIndex: 'name', fieldProps: { placeholder: '支持模糊搜索' } },
          { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
          {
            title: '当前版本',
            search: false,
            render: (_, t) => {
              const current = state.ruleSets.find((r) => r.typeId === t.id && r.isCurrent);
              return current ? `v${current.version} 已发布` : '无发布';
            },
          },
          {
            title: '启用',
            width: 90,
            search: false,
            render: (_, t) => (
              <Switch
                checked={t.enabled}
                onChange={(checked) => {
                  api.setTypeEnabled(t.id, checked);
                  message.success(checked ? '已启用' : '已停用');
                }}
              />
            ),
          },
          {
            title: '操作',
            width: 200,
            search: false,
            render: (_, t) => (
              <Space>
                <Button type="link" onClick={() => navigate(`/rule-sets/${t.id}`)}>
                  定义规则
                </Button>
                <Button
                  type="link"
                  onClick={() => {
                    setEditing(t);
                    form.setFieldsValue(t);
                    setOpen(true);
                  }}
                >
                  编辑
                </Button>
              </Space>
            ),
          },
        ]}
        locale={{ emptyText: '尚无规则集。请先创建。' }}
      />
      <Modal
        title={editing ? '编辑规则集' : '新建规则集'}
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          {editing ? (
            <Form.Item label="编号">
              <Input value={editing.code} disabled />
            </Form.Item>
          ) : (
            <p style={{ color: '#666', marginTop: 0 }}>编号将在创建后由系统自动生成，例如 RS-0004。</p>
          )}
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入规则集名称' }]}>
            <Input placeholder="如 立项报告规则集" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
