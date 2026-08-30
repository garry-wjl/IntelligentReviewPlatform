import { useEffect, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { toTask } from '../../api/adapters';
import { pageEvaluations } from '../../api/evaluation';
import { PassTag, TaskStatusTag } from '../../components/StatusTags';
import type { Auditor, DocType, EvaluationTask } from '../../types/models';
import { auditorDisplay, isTaskLevelAuditor, loadAllRuleSets, loadAuditors, nameMap } from '../../utils/lookups';
import { keywordSearchColumn } from '../../utils/search';

export default function TaskListPage() {
  const navigate = useNavigate();
  const [ruleSets, setRuleSets] = useState<DocType[]>([]);
  const [auditors, setAuditors] = useState<Auditor[]>([]);

  useEffect(() => {
    void Promise.all([loadAllRuleSets(), loadAuditors()]).then(([rs, ads]) => {
      setRuleSets(rs);
      setAuditors(ads);
    });
  }, []);

  const ruleSetNames = nameMap(ruleSets);
  const auditorNames = nameMap(auditors);

  const columns: ProColumns<EvaluationTask>[] = [
    keywordSearchColumn<EvaluationTask>('任务编号或业务单号'),
    {
      title: '任务编号',
      dataIndex: 'id',
      width: 180,
      ellipsis: true,
      search: false,
    },
    {
      title: '业务单号',
      dataIndex: 'bizId',
      search: false,
    },
    {
      title: '种类',
      dataIndex: 'isTrial',
      width: 80,
      search: false,
      valueType: 'select',
      valueEnum: { false: { text: '审核' }, true: { text: '试评' } },
      render: (_, t) => (t.isTrial ? <Tag>试评</Tag> : <Tag color="blue">审核</Tag>),
    },
    {
      title: '规则集',
      dataIndex: 'typeId',
      search: false,
      render: (_, t) => t.typeName ?? t.typeCode ?? '未定',
    },
    {
      title: '审核器',
      dataIndex: 'auditorId',
      search: false,
      render: (_, t) => auditorDisplay(t),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      search: false,
      render: (_, t) => <TaskStatusTag status={t.status} />,
    },
    {
      title: '结果',
      search: false,
      render: (_, t) => <PassTag passed={t.passed} complete={t.complete} />,
    },
    {
      title: '规则版本',
      width: 90,
      search: false,
      render: (_, t) => (t.ruleSetVersion ? `v${t.ruleSetVersion}` : t.isTrial ? '草稿快照' : '—'),
    },
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
  ];

  return (
    <PageContainer
      content="审核任务绑定一次执行所用的规则集版本；审核器在规则上指定。列表用于排查与审计。管理端不提供改分或锁定。"
      extra={<Button type="primary" onClick={() => navigate('/tasks/create')}>创建审核任务</Button>}
    >
      <ProTable<EvaluationTask>
        rowKey="id"
        columns={columns}
        options={false}
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 'auto' }}
        request={async (params) => {
          const page = await pageEvaluations({
            pageNo: params.current,
            pageSize: params.pageSize,
            keyword: params.keyword as string | undefined,
          });
          return {
            data: (page.list ?? []).map((item) =>
              toTask(item, {
                ruleSetName: item.ruleSetNum ? ruleSetNames[item.ruleSetNum] : undefined,
                auditorName: isTaskLevelAuditor(item.auditorNum) ? auditorNames[item.auditorNum] : undefined,
              }),
            ),
            success: true,
            total: page.total ?? 0,
          };
        }}
      />
    </PageContainer>
  );
}
