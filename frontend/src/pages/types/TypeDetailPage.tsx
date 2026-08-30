import { useEffect, useMemo, useState } from 'react';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  message,
} from 'antd';
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { toDocType, toRuleSet, UI_TO_ENGINE, UI_TO_SCORE_MODE } from '../../api/adapters';
import {
  changeScoreMode,
  createDraft,
  getRuleSetDetail,
  getRuleSetVersion,
  moveRule,
  publishRuleSet,
  removeRule,
  setRuleSetEnabled,
  updateRuleSet,
  upsertRule,
} from '../../api/ruleset';
import { RuleSetStatusTag } from '../../components/StatusTags';
import type { Auditor, DocType, PassMode, Rule, RuleEngineKind, RuleSet } from '../../types/models';
import { pageBreadcrumb } from '../../utils/breadcrumb';
import { loadAuditors } from '../../utils/lookups';
import { PASS_MODE_HINT, PASS_MODE_LABEL, ruleCompleteness, validateRuleSet } from '../../utils/ruleDisplay';
import { keywordMatches, keywordSearchColumn } from '../../utils/search';

const emptyRule = (): Rule => ({
  id: '',
  name: '',
  standard: '',
  minScore: 0,
  maxScore: 10,
  passScore: 6,
  weight: 1,
  isVeto: false,
  positiveExample: '',
  negativeExample: '',
  sort: 1,
  engineKind: 'ordinary',
  auditorId: '',
  auditorName: '',
  checks: [],
  agentParamKeys: ['Input', 'Attachment'],
});

function versionLabel(version: RuleSet) {
  if (version.status === 'draft') return '草稿';
  const text = `v${version.version ?? ''}`;
  if (version.isCurrent) return `${text}（当前发布）`;
  if (version.status === 'archived') return `${text}（历史）`;
  return text;
}

function sortVersions(versions: RuleSet[]) {
  return [...versions].sort((a, b) => {
    if (a.status === 'draft' && b.status !== 'draft') return -1;
    if (a.status !== 'draft' && b.status === 'draft') return 1;
    return (b.version ?? 0) - (a.version ?? 0);
  });
}

function pickDefaultVersion(versions: RuleSet[]) {
  const draft = versions.find((item) => item.status === 'draft');
  const current = versions.find((item) => item.isCurrent);
  return draft?.id || current?.id || versions[0]?.id;
}

function inheritSource(versions: RuleSet[], selected?: RuleSet) {
  if (selected && selected.status !== 'draft') return selected.id;
  const current = versions.find((item) => item.isCurrent);
  if (current) return current.id;
  return [...versions]
    .filter((item) => item.version)
    .sort((a, b) => (b.version ?? 0) - (a.version ?? 0))[0]?.id;
}

export default function TypeDetailPage() {
  const { typeId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const versionNum = searchParams.get('version') ?? undefined;
  const [docType, setDocType] = useState<DocType>();
  const [versions, setVersions] = useState<RuleSet[]>([]);
  const [ruleSet, setRuleSet] = useState<RuleSet>();
  const [loading, setLoading] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [current, setCurrent] = useState<Rule | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm<Rule>();
  const [profileForm] = Form.useForm();
  const [auditors, setAuditors] = useState<Auditor[]>([]);
  const auditorId = Form.useWatch('auditorId', form) as string | undefined;
  const selectedAuditor = auditors.find((item) => item.id === auditorId);
  const engineKind: RuleEngineKind = selectedAuditor?.kind ?? 'ordinary';
  const sceneParams = docType?.params ?? [];
  const readonly = ruleSet?.status !== 'draft';
  const passMode = docType?.passMode ?? ruleSet?.passMode ?? 'veto_weighted';
  const overallPassScore = docType?.overallPassScore ?? ruleSet?.overallPassScore ?? 70;
  const errors = useMemo(() => {
    if (!ruleSet) return [];
    return validateRuleSet({
      ...ruleSet,
      passMode: docType?.passMode ?? ruleSet.passMode,
      overallPassScore: docType?.overallPassScore ?? ruleSet.overallPassScore,
    });
  }, [ruleSet, docType]);
  const hasDraft = versions.some((item) => item.status === 'draft');
  const currentPublished = versions.find((item) => item.isCurrent);

  const loadMeta = async () => {
    if (!typeId) return { type: undefined as DocType | undefined, vers: [] as RuleSet[] };
    const detail = await getRuleSetDetail(typeId);
    const type = toDocType(detail);
    const vers = sortVersions((detail.versions ?? []).map(toRuleSet));
    setDocType(type);
    setVersions(vers);
    return { type, vers };
  };

  const loadVersion = async (num: string) => {
    const version = await getRuleSetVersion(num);
    setRuleSet(toRuleSet(version));
  };

  const selectVersion = (num: string) => {
    setSearchParams({ version: num }, { replace: true });
  };

  useEffect(() => {
    void (async () => {
      if (!typeId) return;
      setLoading(true);
      try {
        const { vers } = await loadMeta();
        const preferred = versionNum && vers.some((item) => item.id === versionNum) ? versionNum : pickDefaultVersion(vers);
        if (preferred && preferred !== versionNum) {
          selectVersion(preferred);
        } else if (!preferred) {
          setRuleSet(undefined);
        }
      } catch (e) {
        message.error((e as Error).message);
        setDocType(undefined);
        setVersions([]);
        setRuleSet(undefined);
      } finally {
        setLoading(false);
      }
    })();
  }, [typeId]);

  useEffect(() => {
    void loadAuditors().then(setAuditors).catch((e: Error) => message.error(e.message));
  }, []);

  useEffect(() => {
    if (!versionNum || !typeId) return;
    void loadVersion(versionNum).catch((e: Error) => {
      message.error(e.message);
      setRuleSet(undefined);
    });
  }, [typeId, versionNum]);

  const reload = async () => {
    const { vers } = await loadMeta();
    const next = versionNum && vers.some((item) => item.id === versionNum) ? versionNum : pickDefaultVersion(vers);
    if (next && next !== versionNum) {
      selectVersion(next);
    } else if (next) {
      await loadVersion(next);
    }
  };

  const saveProfile = async () => {
    if (!docType) return;
    const values = await profileForm.validateFields();
    try {
      await updateRuleSet({ num: docType.id, name: values.name, description: values.description });
      message.success('基本信息已保存');
      setEditOpen(false);
      await loadMeta();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const toggleEnabled = async (enabled: boolean) => {
    if (!docType) return;
    try {
      await setRuleSetEnabled({ num: docType.id, enabled });
      message.success(enabled ? '已启用' : '已禁用');
      await loadMeta();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const createNewVersion = async () => {
    if (!docType) return;
    if (hasDraft) {
      message.warning('已有草稿，请先编辑或发布');
      const draft = versions.find((item) => item.status === 'draft');
      if (draft) selectVersion(draft.id);
      return;
    }
    try {
      const draft = await createDraft({
        num: docType.id,
        basedOnVersionNum: inheritSource(versions, ruleSet),
      });
      message.success('已创建新版本，并继承上一版本规则');
      selectVersion(draft.versionNum);
      await loadMeta();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const saveMeta = async (nextPassMode: PassMode, nextOverallPassScore: number) => {
    if (!docType) return;
    try {
      await changeScoreMode({
        num: docType.id,
        scoreMode: UI_TO_SCORE_MODE[nextPassMode],
        overallPassScore: nextOverallPassScore,
      });
      await loadMeta();
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const openRule = (rule?: Rule) => {
    const next = rule ?? emptyRule();
    setCurrent(next);
    form.setFieldsValue(next);
    setDrawerOpen(true);
  };

  const saveRule = async () => {
    if (!docType || !ruleSet) return;
    const values = await form.validateFields();
    const auditor = auditors.find((item) => item.id === values.auditorId);
    const kind: RuleEngineKind = auditor?.kind ?? 'ordinary';
    try {
      await upsertRule({
        num: docType.id,
        versionNum: ruleSet.id,
        ruleNum: current?.id || undefined,
        name: values.name,
        standard: values.standard,
        minScore: values.minScore,
        maxScore: values.maxScore,
        passScore: values.passScore,
        weight: values.weight,
        veto: values.isVeto,
        positiveExample: values.positiveExample,
        negativeExample: values.negativeExample,
        sortNo: current?.sort,
        engineKind: UI_TO_ENGINE[kind],
        auditorNum: values.auditorId,
        checks: kind === 'agent' ? [] : (values.checks ?? []).filter((c) => c?.paramKey && c?.op),
        agentParamKeys: kind === 'agent' ? values.agentParamKeys ?? [] : [],
      });
      message.success('规则已保存');
      setDrawerOpen(false);
      await loadVersion(ruleSet.id);
    } catch (e) {
      message.error((e as Error).message);
    }
  };

  const publish = () => {
    if (!docType) return;
    Modal.confirm({
      title: '发布新版本',
      content: '发布后新任务将使用本版本。进行中的旧任务仍使用其创建时的版本。',
      onOk: async () => {
        try {
          const result = await publishRuleSet({ num: docType.id });
          message.success(`已发布为 v${result.versionNo ?? ''}`);
          await reload();
        } catch (e) {
          message.error((e as Error).message);
          return Promise.reject();
        }
      },
    });
  };

  const crumbs = pageBreadcrumb([
    { title: '审核规则', path: '/rule-sets' },
    { title: docType?.name || (loading ? '规则集详情' : '规则集不存在') },
  ]);

  if (!loading && !docType) {
    return <PageContainer title="规则集不存在" breadcrumb={crumbs} />;
  }

  return (
    <PageContainer
      loading={loading}
      title={docType?.name}
      breadcrumb={crumbs}
      extra={
        docType
          ? [
              <Button
                key="edit"
                onClick={() => {
                  profileForm.setFieldsValue({ name: docType.name, description: docType.description });
                  setEditOpen(true);
                }}
              >
                编辑
              </Button>,
              docType.enabled ? (
                <Popconfirm key="disable" title="禁用后新任务将无法使用该规则集。" onConfirm={() => toggleEnabled(false)}>
                  <Button danger>禁用</Button>
                </Popconfirm>
              ) : (
                <Button key="enable" type="primary" onClick={() => toggleEnabled(true)}>
                  启用
                </Button>
              ),
            ]
          : undefined
      }
    >
      <Card title="基本信息" style={{ marginBottom: 16 }}>
        <Descriptions column={{ xs: 1, sm: 2, md: 3 }} size="small">
          <Descriptions.Item label="规则集名称">{docType?.name}</Descriptions.Item>
          <Descriptions.Item label="编号">{docType?.code}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={docType?.enabled ? 'success' : 'default'}>{docType?.enabled ? '启用' : '禁用'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="场景">{docType?.sceneName || docType?.sceneNum || '—'}</Descriptions.Item>
          <Descriptions.Item label="当前发布">
            {currentPublished?.version ? `v${currentPublished.version}` : '无'}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">{docType?.updatedAt || '—'}</Descriptions.Item>
          <Descriptions.Item label="说明" span={3}>
            {docType?.description || '—'}
          </Descriptions.Item>
        </Descriptions>
        <Typography.Title level={5} style={{ marginTop: 16 }}>
          评估分方式
        </Typography.Title>
        <Radio.Group
          value={passMode}
          onChange={(e) => {
            Modal.confirm({
              title: '切换评估分方式',
              content: '「全部通过」不使用权重/红线；「加权总分」需要权重；「红线 + 加权」需要红线条且红线不计入加权。新任务按规则集当前方式打分；已创建任务仍用其快照。',
              onOk: () => saveMeta(e.target.value, overallPassScore),
            });
          }}
        >
          {(Object.keys(PASS_MODE_LABEL) as PassMode[]).map((key) => (
            <Radio key={key} value={key}>
              {PASS_MODE_LABEL[key]}
            </Radio>
          ))}
        </Radio.Group>
        <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>
          {PASS_MODE_HINT[passMode]}
        </Typography.Paragraph>
        {passMode !== 'all_pass' ? (
          <Space>
            <span>总分通过线</span>
            <InputNumber
              min={0}
              value={overallPassScore}
              onChange={(value) => saveMeta(passMode, Number(value || 0))}
            />
          </Space>
        ) : null}
      </Card>

      <Card
        title="审核规则"
        extra={
          <Space wrap>
            <span>版本</span>
            <Select
              style={{ minWidth: 200 }}
              value={versionNum}
              options={versions.map((item) => ({ value: item.id, label: versionLabel(item) }))}
              onChange={(value) => selectVersion(value)}
              placeholder="请选择版本"
            />
            {!hasDraft ? (
              <Button onClick={() => void createNewVersion()}>创建新版本</Button>
            ) : null}
            {ruleSet?.status === 'draft' ? (
              <Button type="primary" disabled={errors.length > 0} onClick={publish}>
                发布
              </Button>
            ) : null}
            {ruleSet ? (
              <Button onClick={() => navigate(`/rule-sets/${docType!.id}/versions/${ruleSet.id}/trial`)}>试评</Button>
            ) : null}
          </Space>
        }
      >
        {!ruleSet ? (
          <Typography.Paragraph type="secondary">暂无版本。请创建新版本后添加规则。</Typography.Paragraph>
        ) : (
          <>
            {readonly ? (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message="已发布版本只读。如需修改，请创建新版本（会自动继承当前版本规则）。"
              />
            ) : null}
            {!readonly && errors.length ? (
              <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="发布前请处理" description={errors.join('；')} />
            ) : null}
            {ruleSet.basedOnVersion ? (
              <Typography.Paragraph type="secondary">本草稿继承自 v{ruleSet.basedOnVersion}</Typography.Paragraph>
            ) : null}

            <Space style={{ marginBottom: 12 }}>
              <Typography.Title level={5} style={{ margin: 0 }}>
                规则清单
              </Typography.Title>
              <RuleSetStatusTag status={ruleSet.status} isCurrent={ruleSet.isCurrent} />
              {!readonly ? (
                <Button type="dashed" onClick={() => openRule()}>
                  添加规则
                </Button>
              ) : null}
            </Space>

            <ProTable<Rule>
              rowKey="id"
              size="small"
              search={{ labelWidth: 'auto' }}
              options={false}
              pagination={false}
              params={{ versionNum: ruleSet.id, stamp: ruleSet.rules.map((r) => r.id).join(',') }}
              request={async (params) => {
                const list = [...ruleSet.rules]
                  .sort((a, b) => a.sort - b.sort)
                  .filter((r) => keywordMatches(params.keyword as string | undefined, r.id, r.name));
                return { data: list, success: true, total: list.length };
              }}
              columns={[
                keywordSearchColumn<Rule>(),
                { title: '编号', dataIndex: 'id', width: 140, ellipsis: true, search: false },
                { title: '序', dataIndex: 'sort', width: 50, search: false },
                { title: '名称', dataIndex: 'name', search: false },
                {
                  title: '审核器',
                  width: 180,
                  search: false,
                  render: (_, r) => r.auditorName || r.auditorId || '未指定',
                },
                { title: '区间', search: false, render: (_, r) => `${r.minScore}-${r.maxScore}`, width: 90 },
                { title: '通过分', dataIndex: 'passScore', width: 80, search: false },
                { title: '权重', dataIndex: 'weight', width: 70, search: false },
                { title: '红线', width: 70, search: false, render: (_, r) => (r.isVeto ? <Tag color="red">是</Tag> : '否') },
                { title: '状态', width: 100, search: false, render: (_, r) => <Tag>{ruleCompleteness(r)}</Tag> },
                {
                  title: '操作',
                  width: 220,
                  search: false,
                  render: (_, r) => (
                    <Space>
                      <Button type="link" onClick={() => openRule(r)}>
                        {readonly ? '查看' : '编辑'}
                      </Button>
                      {!readonly ? (
                        <>
                          <Button
                            type="link"
                            onClick={async () => {
                              try {
                                await moveRule({ num: docType!.id, versionNum: ruleSet.id, ruleNum: r.id, direction: -1 });
                                await loadVersion(ruleSet.id);
                              } catch (e) {
                                message.error((e as Error).message);
                              }
                            }}
                          >
                            上移
                          </Button>
                          <Button
                            type="link"
                            onClick={async () => {
                              try {
                                await moveRule({ num: docType!.id, versionNum: ruleSet.id, ruleNum: r.id, direction: 1 });
                                await loadVersion(ruleSet.id);
                              } catch (e) {
                                message.error((e as Error).message);
                              }
                            }}
                          >
                            下移
                          </Button>
                          <Popconfirm
                            title="删除该规则？"
                            onConfirm={async () => {
                              try {
                                await removeRule({ num: docType!.id, versionNum: ruleSet.id, ruleNum: r.id });
                                await loadVersion(ruleSet.id);
                              } catch (e) {
                                message.error((e as Error).message);
                              }
                            }}
                          >
                            <Button type="link" danger>
                              删除
                            </Button>
                          </Popconfirm>
                        </>
                      ) : null}
                    </Space>
                  ),
                },
              ]}
            />
          </>
        )}
      </Card>

      <Modal
        title="编辑规则集"
        open={editOpen}
        onOk={saveProfile}
        onCancel={() => setEditOpen(false)}
        destroyOnHidden
      >
        <Form form={profileForm} layout="vertical">
          <Form.Item label="编号">
            <Input value={docType?.code} disabled />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入规则集名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="场景">
            <Input value={docType?.sceneName || docType?.sceneNum || '—'} disabled />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={current ? `规则 · ${current.name || '未命名'}` : '规则'}
        width={640}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          readonly ? null : (
            <Button type="primary" onClick={saveRule}>
              保存
            </Button>
          )
        }
      >
        <Form form={form} layout="vertical" disabled={readonly}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="standard" label="评审标准（写给人看的标准，不用写提示词）" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item
            name="auditorId"
            label="审核器"
            rules={[{ required: true, message: '请选择审核器' }]}
            extra="普通审核器走判空/长度等校验；Agent 审核器把所选场景参数交给 Agent。"
          >
            <Select
              placeholder="请选择审核器"
              options={auditors
                .filter((item) => item.enabled || item.id === current?.auditorId)
                .map((item) => ({
                  value: item.id,
                  label: `${item.name}（${item.kind === 'agent' ? 'Agent' : '普通'}）`,
                }))}
            />
          </Form.Item>
          {engineKind === 'ordinary' ? (
            <>
              <Typography.Paragraph type="secondary">对场景参数做判空、长度等常规校验。不调用 Agent。</Typography.Paragraph>
              <Form.List name="checks">
                {(fields, { add, remove }) => (
                  <>
                    {fields.map((field) => (
                      <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }} wrap>
                        <Form.Item {...field} name={[field.name, 'paramKey']} rules={[{ required: true, message: '参数' }]}>
                          <Select
                            style={{ width: 160 }}
                            placeholder="参数"
                            options={sceneParams.map((p) => ({ value: p.key, label: `${p.label}（${p.key}）` }))}
                          />
                        </Form.Item>
                        <Form.Item {...field} name={[field.name, 'op']} rules={[{ required: true, message: '操作' }]}>
                          <Select
                            style={{ width: 140 }}
                            options={[
                              { value: 'NOT_BLANK', label: '不能为空' },
                              { value: 'MIN_LENGTH', label: '最小长度' },
                              { value: 'MAX_LENGTH', label: '最大长度' },
                              { value: 'REGEX', label: '正则匹配' },
                            ]}
                          />
                        </Form.Item>
                        <Form.Item {...field} name={[field.name, 'value']}>
                          <Input style={{ width: 140 }} placeholder="比较值（可选）" />
                        </Form.Item>
                        <MinusCircleOutlined onClick={() => remove(field.name)} />
                      </Space>
                    ))}
                    <Button type="dashed" onClick={() => add({ op: 'NOT_BLANK' })} block icon={<PlusOutlined />}>
                      添加校验
                    </Button>
                  </>
                )}
              </Form.List>
            </>
          ) : (
            <Form.Item
              name="agentParamKeys"
              label="传给 Agent 的参数"
              rules={[{ required: true, type: 'array', min: 1, message: '请至少选择一个参数' }]}
              extra="Agent 定义与返回结构不在本平台维护，默认认为会按所选参数完成匹配并返回对应结果。"
            >
              <Checkbox.Group
                options={sceneParams.map((p) => ({
                  value: p.key,
                  label: `${p.label}（${p.key}${p.builtin ? ' · 内置' : ''}）`,
                }))}
              />
            </Form.Item>
          )}
          <Space>
            <Form.Item name="minScore" label="最低分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="maxScore" label="最高分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="passScore" label="通过分" rules={[{ required: true }]}>
              <InputNumber />
            </Form.Item>
            <Form.Item name="weight" label="权重">
              <InputNumber min={0} />
            </Form.Item>
          </Space>
          <Form.Item name="isVeto" label="红线" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="positiveExample" label="正例（什么样算高分，选填）">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="negativeExample" label="反例（什么样算低分，选填）">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Drawer>
    </PageContainer>
  );
}
