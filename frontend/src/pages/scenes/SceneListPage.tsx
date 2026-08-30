import { useRef, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Space, Switch, Tag, Typography, message } from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { toScene } from '../../api/adapters';
import { createScene, pageScenes, setSceneEnabled, updateScene } from '../../api/scene';
import type { Scene } from '../../types/models';
import { keywordSearchColumn } from '../../utils/search';

export default function SceneListPage() {
  const actionRef = useRef<ActionType>();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Scene | null>(null);
  const [form] = Form.useForm();

  const submit = async () => {
    const values = await form.validateFields();
    try {
      const extraParams = ((values.extraParams as { key?: string; label?: string }[]) ?? [])
        .filter((item) => item?.key)
        .map((item) => ({ key: item.key!.trim(), label: (item.label || item.key!).trim() }));
      const payload = {
        name: values.name as string,
        description: values.description as string | undefined,
        extraParams,
      };
      if (editing) {
        await updateScene({ num: editing.id, ...payload });
        message.success('已保存');
      } else {
        const created = await createScene(payload);
        message.success(`已创建，系统编号 ${created.num}`);
      }
      setOpen(false);
      actionRef.current?.reload();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const columns: ProColumns<Scene>[] = [
    keywordSearchColumn<Scene>(),
    { title: '编号', dataIndex: 'id', width: 140, search: false },
    { title: '名称', dataIndex: 'name', search: false },
    { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
    {
      title: '参数',
      search: false,
      render: (_, s) => (
        <Space wrap size={[4, 4]}>
          {(s.params ?? []).map((p) => (
            <Tag key={p.key} color={p.builtin ? 'blue' : 'default'}>
              {p.key}
              {p.builtin ? '（内置）' : ''}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '启用',
      width: 90,
      search: false,
      render: (_, s) => (
        <Switch
          checked={s.enabled}
          onChange={async (v) => {
            try {
              await setSceneEnabled({ num: s.id, enabled: v });
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
      render: (_, s) => (
        <Button
          type="link"
          onClick={() => {
            setEditing(s);
            form.setFieldsValue({
              name: s.name,
              description: s.description,
              extraParams: s.extraParams.length ? s.extraParams : [],
            });
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
      content="场景定义一次审核要评什么。每个场景内置 Input（不限长度文本）和 Attachment（附件对象数组：唯一标识、名称、URL），还可声明扩展参数。创建规则集时必须选择场景，参数会自动带出。"
      extra={
        <Button
          type="primary"
          onClick={() => {
            setEditing(null);
            form.resetFields();
            form.setFieldsValue({ extraParams: [] });
            setOpen(true);
          }}
        >
          新建场景
        </Button>
      }
    >
      <ProTable<Scene>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        options={false}
        search={{ labelWidth: 'auto' }}
        pagination={{ pageSize: 10 }}
        request={async (params) => {
          const page = await pageScenes({
            pageNo: params.current,
            pageSize: params.pageSize,
            keyword: params.keyword as string | undefined,
          });
          return { data: (page.list ?? []).map(toScene), success: true, total: page.total ?? 0 };
        }}
        locale={{ emptyText: '尚无场景。请先创建。' }}
      />
      <Modal
        title={editing ? '编辑场景' : '新建场景'}
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        destroyOnHidden
        width={640}
      >
        <Form form={form} layout="vertical">
          {editing ? (
            <Form.Item label="编号">
              <Input value={editing.id} disabled />
            </Form.Item>
          ) : (
            <p style={{ color: '#666', marginTop: 0 }}>编号将在创建后由系统自动生成。</p>
          )}
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入场景名称' }]}>
            <Input placeholder="如 立项材料审核" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            内置默认参数：Input（用户输入，不限长度）、Attachment（附件数组，含唯一标识 / 名称 / URL）。这两项不可删除。
          </Typography.Paragraph>
          <Form.List name="extraParams">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                    <Form.Item
                      {...field}
                      name={[field.name, 'key']}
                      rules={[{ required: true, message: '请填写参数键' }]}
                    >
                      <Input placeholder="参数键，如 projectName" />
                    </Form.Item>
                    <Form.Item
                      {...field}
                      name={[field.name, 'label']}
                      rules={[{ required: true, message: '请填写名称' }]}
                    >
                      <Input placeholder="展示名称" />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(field.name)} />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加扩展参数
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </PageContainer>
  );
}
