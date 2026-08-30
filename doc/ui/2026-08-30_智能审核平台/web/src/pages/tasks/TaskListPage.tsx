import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Button, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAppStore } from '../../mock/store';
import { PassTag, TaskStatusTag, TypeSourceTag } from '../../components/StatusTags';
import type { EvaluationTask } from '../../types/models';
import { TASK_STATUS_META } from '../../components/StatusTags';
import { textIncludes } from '../../utils/search';

export default function TaskListPage() {
  const state = useAppStore();
  const navigate = useNavigate();

  return (
    <PageContainer
      content="审核任务绑定一次执行所用的规则集版本与审核器。列表用于排查与审计，不是给审批人用的复核台。"
      extra={<Button type="primary" onClick={() => navigate('/tasks/create')}>创建审核任务</Button>}
    >
      <ProTable<EvaluationTask>
        rowKey="id"
        options={false}
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 'auto' }}
        params={{ stamp: state.tasks.map((t) => t.id).join(',') }}
        request={async (params) => {
          const list = state.tasks.filter(
            (t) => textIncludes(t.id, params.id as string) && textIncludes(t.bizId, params.bizId as string),
          );
          return { data: list, success: true, total: list.length };
        }}
        columns={[
          {
            title: '任务 ID',
            dataIndex: 'id',
            width: 180,
            ellipsis: true,
            formItemProps: { label: '编号' },
            fieldProps: { placeholder: '支持模糊搜索' },
          },
          {
            title: '业务单号',
            dataIndex: 'bizId',
            formItemProps: { label: '名称' },
            fieldProps: { placeholder: '按业务单号模糊搜索' },
          },
          {
            title: '种类',
            width: 80,
            search: false,
            render: (_, t) => (t.isTrial ? <Tag>试评</Tag> : <Tag color="blue">审核</Tag>),
            filters: [
              { text: '审核', value: 'live' },
              { text: '试评', value: 'trial' },
            ],
            onFilter: (value, t) => (value === 'trial' ? t.isTrial : !t.isTrial),
          },
          { title: '规则集', dataIndex: 'typeName', search: false, render: (_, t) => t.typeName ?? '未定' },
          {
            title: '审核器',
            search: false,
            render: (_, t) => t.auditorName ?? '—',
          },
          {
            title: '来源',
            width: 110,
            search: false,
            render: (_, t) => <TypeSourceTag source={t.typeSource} />,
          },
          {
            title: '状态',
            width: 120,
            search: false,
            render: (_, t) => <TaskStatusTag status={t.status} />,
            filters: Object.entries(TASK_STATUS_META).map(([value, meta]) => ({ text: meta.text, value })),
            onFilter: (value, t) => t.status === value,
          },
          {
            title: '结果',
            search: false,
            render: (_, t) => <PassTag passed={t.passed} complete={t.complete} />,
          },
          { title: '规则版本', width: 90, search: false, render: (_, t) => (t.ruleSetVersion ? `v${t.ruleSetVersion}` : t.isTrial ? '草稿快照' : '—') },
          { title: '创建时间', dataIndex: 'createdAt', width: 180, search: false },
          {
            title: '操作',
            width: 90,
            search: false,
            render: (_, t) => (
              <Button type="link" onClick={() => navigate(`/tasks/${t.id}`)}>
                详情
              </Button>
            ),
          },
        ]}
      />
    </PageContainer>
  );
}
