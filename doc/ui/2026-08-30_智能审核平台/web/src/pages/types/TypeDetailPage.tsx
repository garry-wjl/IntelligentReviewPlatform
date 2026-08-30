import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Typography, message } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { api, useAppStore } from '../../mock/store';
import { PASS_MODE_LABEL } from '../../mock/engine';
import { RuleSetStatusTag } from '../../components/StatusTags';
import type { RuleSet } from '../../types/models';

export default function TypeDetailPage() {
  const { typeId } = useParams();
  const state = useAppStore();
  const navigate = useNavigate();
  const docType = state.types.find((t) => t.id === typeId);
  const ruleSets = typeId ? api.listRuleSets(typeId) : [];

  if (!docType) {
    return <PageContainer title="规则集不存在" />;
  }

  return (
    <PageContainer
      title={docType.name}
      subTitle={docType.code}
      content="在此维护该规则集的版本：用自然语言定义规则，并配置评估分方式（全部通过 / 加权总分 / 红线+加权）。"
      extra={[
        <Button
          key="draft"
          type="primary"
          onClick={() => {
            const draft = api.createDraft(docType.id);
            message.success('已创建草稿');
            navigate(`/rule-sets/${docType.id}/versions/${draft.id}`);
          }}
        >
          新建草稿
        </Button>,
        <Popconfirm
          key="disable"
          title="停用当前发布后，新任务将无法使用该规则集。"
          onConfirm={() => {
            api.disableCurrent(docType.id);
            message.success('已停用当前发布');
          }}
        >
          <Button danger>停用当前发布</Button>
        </Popconfirm>,
      ]}
    >
      <Typography.Paragraph type="secondary">同一规则集仅一个当前已发布版本供新任务使用；历史版本只读，可复制为新草稿。</Typography.Paragraph>
      <ProTable<RuleSet>
        rowKey="id"
        search={false}
        options={false}
        pagination={false}
        dataSource={ruleSets}
        columns={[
          {
            title: '版本',
            render: (_, r) => (r.version ? `v${r.version}` : '草稿'),
            width: 90,
          },
          {
            title: '状态',
            render: (_, r) => <RuleSetStatusTag status={r.status} isCurrent={r.isCurrent} />,
            width: 120,
          },
          {
            title: '评估分方式',
            render: (_, r) => PASS_MODE_LABEL[r.passMode],
          },
          { title: '规则数', render: (_, r) => r.rules.length, width: 80 },
          { title: '基于', render: (_, r) => (r.basedOnVersion ? `v${r.basedOnVersion}` : '—'), width: 80 },
          { title: '发布时间', dataIndex: 'publishedAt', width: 180 },
          {
            title: '操作',
            width: 260,
            render: (_, r) => (
              <Space>
                <Button type="link" onClick={() => navigate(`/rule-sets/${docType.id}/versions/${r.id}`)}>
                  {r.status === 'draft' ? '编辑' : '查看'}
                </Button>
                <Button type="link" onClick={() => navigate(`/rule-sets/${docType.id}/versions/${r.id}/trial`)}>
                  试评
                </Button>
                {r.status !== 'draft' ? (
                  <Button
                    type="link"
                    onClick={() => {
                      const draft = api.createDraft(docType.id, r.id);
                      message.success(`已复制 v${r.version} 为新草稿`);
                      navigate(`/rule-sets/${docType.id}/versions/${draft.id}`);
                    }}
                  >
                    复制为新草稿
                  </Button>
                ) : null}
              </Space>
            ),
          },
        ]}
      />
    </PageContainer>
  );
}
