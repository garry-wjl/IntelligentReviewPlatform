import { useEffect, useRef, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Select, Space, Tag, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { toDocType, toScene } from '../../api/adapters';
import { createRuleSet, pageRuleSets } from '../../api/ruleset';
import { listEnabledScenes } from '../../api/scene';
import type { DocType, Scene } from '../../types/models';
import { keywordSearchColumn } from '../../utils/search';

export default function TypeListPage() {
  const navigate = useNavigate();
  const actionRef = useRef<ActionType>();
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const [scenes, setScenes] = useState<Scene[]>([]);
  const sceneNum = Form.useWatch('sceneNum', form);
  const selectedScene = scenes.find((s) => s.id === sceneNum);

  useEffect(() => {
    void listEnabledScenes()
      .then((list) => setScenes(list.map(toScene)))
      .catch((e: Error) => message.error(e.message));
  }, []);

  const submit = async () => {
    const values = await form.validateFields();
    try {
      const created = await createRuleSet({
        name: values.name,
        description: values.description,
        sceneNum: values.sceneNum,
      });
      message.success(`已创建，系统编号 ${created.num}`);
      setOpen(false);
      navigate(`/rule-sets/${created.num}`);
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const columns: ProColumns<DocType>[] = [
    keywordSearchColumn<DocType>(),
    { title: '编号', dataIndex: 'code', width: 140, search: false },
    { title: '名称', dataIndex: 'name', search: false },
    { title: '场景', dataIndex: 'sceneName', width: 160, search: false, ellipsis: true },
    {
      title: '当前版本',
      width: 110,
      search: false,
      render: (_, t) => (t.currentVersionNo ? `v${t.currentVersionNo}` : '草稿'),
    },
    { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
    {
      title: '状态',
      width: 90,
      search: false,
      render: (_, t) => (
        <Tag color={t.enabled ? 'success' : 'default'}>{t.enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    { title: '更新时间', dataIndex: 'updatedAt', width: 200, search: false },
    {
      title: '操作',
      width: 90,
      search: false,
      render: (_, t) => (
        <Button type="link" onClick={() => navigate(`/rule-sets/${t.id}`)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <PageContainer
      content="创建规则集必须选择场景，系统会带出该场景的 Input、Attachment 与扩展参数，供定义规则时选用。"
      extra={
        <Button
          type="primary"
          onClick={() => {
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
        actionRef={actionRef}
        columns={columns}
        options={false}
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 'auto' }}
        request={async (params) => {
          const page = await pageRuleSets({
            pageNo: params.current,
            pageSize: params.pageSize,
            keyword: params.keyword as string | undefined,
          });
          return {
            data: (page.list ?? []).map(toDocType),
            success: true,
            total: page.total ?? 0,
          };
        }}
        locale={{ emptyText: '尚无规则集。请先创建。' }}
      />
      <Modal
        title="新建规则集"
        open={open}
        onOk={submit}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <p style={{ color: '#666', marginTop: 0 }}>编号将在创建后由系统自动生成。</p>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入规则集名称' }]}>
            <Input placeholder="如 立项报告规则集" />
          </Form.Item>
          <Form.Item name="sceneNum" label="场景" rules={[{ required: true, message: '请选择场景' }]}>
            <Select
              placeholder="请选择场景"
              options={scenes.map((s) => ({ value: s.id, label: `${s.name}（${s.id}）` }))}
            />
          </Form.Item>
          {selectedScene ? (
            <div style={{ marginBottom: 16 }}>
              <Typography.Text type="secondary">将自动带出参数：</Typography.Text>
              <div style={{ marginTop: 8 }}>
                <Space wrap>
                  {selectedScene.params.map((p) => (
                    <Tag key={p.key} color={p.builtin ? 'blue' : 'default'}>
                      {p.label}（{p.key}）
                    </Tag>
                  ))}
                </Space>
              </div>
            </div>
          ) : null}
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
