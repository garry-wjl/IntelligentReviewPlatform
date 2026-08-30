import { PageContainer, ProCard, ProTable } from '@ant-design/pro-components';
import { Alert, Button, Checkbox, Form, Input, InputNumber, Modal, Switch, Tag, Typography, message } from 'antd';
import { api, useAppStore } from '../../mock/store';
import type { WebhookEventName, WebhookLog } from '../../types/models';
import { textIncludes } from '../../utils/search';

const EVENTS: { value: WebhookEventName; label: string }[] = [
  { value: 'evaluation.classified', label: 'classified 识别' },
  { value: 'evaluation.scored', label: 'scored 机评完成' },
  { value: 'evaluation.updated', label: 'updated 改分/标注' },
  { value: 'evaluation.finalized', label: 'finalized 锁定' },
  { value: 'evaluation.failed', label: 'failed 失败' },
];

export default function SettingsPage() {
  const state = useAppStore();
  const [form] = Form.useForm();

  return (
    <PageContainer
      extra={
        <Button
          danger
          onClick={() => {
            Modal.confirm({
              title: '重置全部 Mock 数据？',
              onOk: () => {
                api.reset();
                message.success('已恢复演示数据');
              },
            });
          }}
        >
          重置演示数据
        </Button>
      }
    >
      <ProCard title="接入凭证" extra={
        <Button
          type="primary"
          onClick={() => {
            const item = api.createCredential('新接入方');
            Modal.success({
              title: '请立即复制密钥，关闭后不再显示',
              content: <Typography.Paragraph copyable>{item.lastSecret}</Typography.Paragraph>,
              onOk: () => api.clearCredentialSecret(item.id),
            });
          }}
        >
          颁发凭证
        </Button>
      }>
        <ProTable
          rowKey="id"
          search={{ labelWidth: 'auto' }}
          options={false}
          pagination={{ pageSize: 10 }}
          params={{ stamp: state.credentials.map((c) => c.id).join(',') }}
          request={async (params) => {
            const list = state.credentials.filter(
              (c) => textIncludes(c.id, params.id as string) && textIncludes(c.name, params.name as string),
            );
            return { data: list, success: true, total: list.length };
          }}
          columns={[
            { title: '编号', dataIndex: 'id', width: 140, fieldProps: { placeholder: '支持模糊搜索' } },
            { title: '名称', dataIndex: 'name', fieldProps: { placeholder: '支持模糊搜索' } },
            { title: 'Key', dataIndex: 'keyPrefix', search: false },
            {
              title: '状态',
              search: false,
              render: (_, c) => <Tag color={c.enabled ? 'success' : 'default'}>{c.enabled ? '启用' : '停用'}</Tag>,
            },
            { title: '创建时间', dataIndex: 'createdAt', search: false },
            {
              title: '操作',
              search: false,
              render: (_, c) => (
                <Switch
                  checked={c.enabled}
                  onChange={(v) => {
                    api.setCredentialEnabled(c.id, v);
                    message.success(v ? '已启用' : '已停用');
                  }}
                />
              ),
            },
          ]}
        />
      </ProCard>

      <ProCard title="回调与事件订阅" style={{ marginTop: 16 }}>
        <Form
          layout="vertical"
          form={form}
          initialValues={state.settings}
          onValuesChange={(_, all) => api.updateSettings(all)}
        >
          <Form.Item name="callbackUrl" label="默认回调 URL">
            <Input placeholder="https://" />
          </Form.Item>
          <Form.Item name="subscribedEvents" label="订阅事件">
            <Checkbox.Group options={EVENTS} />
          </Form.Item>
          <Form.Item name="classifyThreshold" label="自动识别置信度阈值（低于则待确认类型）">
            <InputNumber min={0} max={1} step={0.05} />
          </Form.Item>
          <Form.Item name="simulateWebhookFail" label="模拟 Webhook 投递失败" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
        <Alert type="info" showIcon message="最少订阅 scored / finalized / failed 即可对接审批节点。试评任务不会回调。" />
      </ProCard>

      <ProCard title="Webhook 投递记录 / 死信" style={{ marginTop: 16 }}>
        <ProTable<WebhookLog>
          rowKey="id"
          search={{ labelWidth: 'auto' }}
          options={false}
          pagination={{ pageSize: 8 }}
          params={{ stamp: state.webhookLogs.map((w) => w.id).join(',') }}
          request={async (params) => {
            const list = state.webhookLogs.filter(
              (w) => textIncludes(w.id, params.id as string) && textIncludes(w.bizId, params.bizId as string),
            );
            return { data: list, success: true, total: list.length };
          }}
          columns={[
            { title: '编号', dataIndex: 'id', width: 160, ellipsis: true, fieldProps: { placeholder: '支持模糊搜索' } },
            { title: '时间', dataIndex: 'at', width: 180, search: false },
            { title: '事件', dataIndex: 'event', search: false },
            {
              title: '业务单号',
              dataIndex: 'bizId',
              formItemProps: { label: '名称' },
              fieldProps: { placeholder: '按业务单号模糊搜索' },
            },
            {
              title: '投递',
              width: 100,
              search: false,
              render: (_, w) => (
                <Tag color={w.status === 'success' ? 'success' : w.status === 'failed' ? 'error' : 'default'}>{w.status}</Tag>
              ),
            },
            { title: '说明', dataIndex: 'error', search: false },
            {
              title: '操作',
              width: 100,
              search: false,
              render: (_, w) =>
                w.status === 'failed' ? (
                  <Button type="link" onClick={() => api.retryWebhook(w.id)}>
                    重试
                  </Button>
                ) : null,
            },
          ]}
        />
      </ProCard>
    </PageContainer>
  );
}
