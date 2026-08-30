import { useEffect, useRef } from 'react';
import { PageContainer, ProCard, ProTable } from '@ant-design/pro-components';
import type { ActionType } from '@ant-design/pro-components';
import { Alert, Button, Checkbox, Form, Input, InputNumber, Modal, Switch, Tag, Typography, message } from 'antd';
import { createCredential, disableCredential, getIntegration, pageCredentials, pageWebhooks, replayWebhook, updateIntegration } from '../../api/access';
import { toCredential, toIntegration, toWebhookLog } from '../../api/adapters';
import type { WebhookEventName, WebhookLog } from '../../types/models';
import { pageBreadcrumb } from '../../utils/breadcrumb';
import { keywordSearchColumn } from '../../utils/search';

const EVENTS: { value: WebhookEventName; label: string }[] = [
  { value: 'evaluation.classified', label: 'classified 识别' },
  { value: 'evaluation.scored', label: 'scored 机评完成' },
  { value: 'evaluation.updated', label: 'updated 改分/标注' },
  { value: 'evaluation.finalized', label: 'finalized 锁定' },
  { value: 'evaluation.failed', label: 'failed 失败' },
];

export default function SettingsPage() {
  const [form] = Form.useForm();
  const credRef = useRef<ActionType>();
  const hookRef = useRef<ActionType>();

  useEffect(() => {
    void getIntegration()
      .then((dto) => form.setFieldsValue(toIntegration(dto)))
      .catch((e: Error) => message.error(e.message));
  }, [form]);

  return (
    <PageContainer
      title="接入设置"
      breadcrumb={pageBreadcrumb([
        { title: '工作台', path: '/' },
        { title: '接入设置' },
      ])}
    >
      <ProCard
        title="接入凭证"
        extra={
          <Button
            type="primary"
            onClick={async () => {
              try {
                const item = await createCredential({ name: '新接入方' });
                Modal.success({
                  title: '请立即复制密钥，关闭后不再显示',
                  content: <Typography.Paragraph copyable>{item.rawSecret}</Typography.Paragraph>,
                });
                credRef.current?.reload();
              } catch (e) {
                message.error((e as Error).message);
              }
            }}
          >
            颁发凭证
          </Button>
        }
      >
        <ProTable
          rowKey="id"
          actionRef={credRef}
          search={{ labelWidth: 'auto' }}
          options={false}
          pagination={{ pageSize: 10 }}
          request={async (params) => {
            const page = await pageCredentials({
              pageNo: params.current,
              pageSize: params.pageSize,
              keyword: params.keyword as string | undefined,
            });
            return { data: (page.list ?? []).map(toCredential), success: true, total: page.total ?? 0 };
          }}
          columns={[
            keywordSearchColumn(),
            { title: '编号', dataIndex: 'id', width: 140, search: false },
            { title: '名称', dataIndex: 'name', search: false },
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
                  disabled={!c.enabled}
                  onChange={async (v) => {
                    if (v) return;
                    try {
                      await disableCredential({ num: c.id });
                      message.success('已停用');
                      credRef.current?.reload();
                    } catch (e) {
                      message.error((e as Error).message);
                    }
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
          onFinish={async (all) => {
            try {
              await updateIntegration({
                callbackUrl: all.callbackUrl,
                subscribedEvents: (all.subscribedEvents as string[] | undefined)?.join(','),
                classifyThreshold: all.classifyThreshold,
              });
              message.success('已保存接入配置');
            } catch (e) {
              message.error((e as Error).message);
            }
          }}
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
          <Button type="primary" htmlType="submit">
            保存接入配置
          </Button>
        </Form>
        <Alert type="info" showIcon style={{ marginTop: 12 }} message="最少订阅 scored / finalized / failed 即可对接审批节点。试评任务不会回调。" />
      </ProCard>

      <ProCard title="Webhook 投递记录 / 死信" style={{ marginTop: 16 }}>
        <ProTable<WebhookLog>
          rowKey="id"
          actionRef={hookRef}
          search={{ labelWidth: 'auto' }}
          options={false}
          pagination={{ pageSize: 8 }}
          request={async (params) => {
            const page = await pageWebhooks({
              pageNo: params.current,
              pageSize: params.pageSize,
              keyword: params.keyword as string | undefined,
            });
            return { data: (page.list ?? []).map(toWebhookLog), success: true, total: page.total ?? 0 };
          }}
          columns={[
            keywordSearchColumn('编号或业务单号'),
            { title: '编号', dataIndex: 'id', width: 160, ellipsis: true, search: false },
            { title: '时间', dataIndex: 'at', width: 180, search: false },
            { title: '事件', dataIndex: 'event', search: false },
            {
              title: '业务单号',
              dataIndex: 'bizId',
              search: false,
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
                  <Button
                    type="link"
                    onClick={async () => {
                      try {
                        await replayWebhook({ num: w.id });
                        message.success('已重试');
                        hookRef.current?.reload();
                      } catch (e) {
                        message.error((e as Error).message);
                      }
                    }}
                  >
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
