import { useSyncExternalStore } from 'react';
import dayjs from 'dayjs';
import type {
  Annotation,
  AppState,
  Attachment,
  Auditor,
  Credential,
  DocType,
  EvaluationTask,
  Rule,
  RuleSet,
  WebhookEventName,
  WebhookLog,
} from '../types/models';
import { createSeed, STORAGE_KEY } from './seed';
import {
  applySummary,
  classifyFromFiles,
  displayScore,
  isUnparseable,
  snapshotResults,
  validateRule,
  validateRuleSet,
} from './engine';

const listeners = new Set<() => void>();
const pipelineTokens = new Map<string, number>();

function now() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function uid(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 8)}${Date.now().toString(36).slice(-4)}`;
}

function emit() {
  persist(state);
  listeners.forEach((l) => l());
}

function persist(s: AppState) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
}

function loadState(): AppState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as AppState;
  } catch {
    /* ignore */
  }
  const seed = withDemoTasks(createSeed());
  persist(seed);
  return seed;
}

let state: AppState = loadState();

function getState() {
  return state;
}

export function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useAppStore() {
  return useSyncExternalStore(subscribe, getState, getState);
}

function patch(updater: (prev: AppState) => AppState) {
  state = updater(state);
  emit();
}

function delay(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function nextCatalogCode() {
  const nums = state.types.map((t) => {
    const m = t.code.match(/^RS-(\d+)$/);
    return m ? Number(m[1]) : 0;
  });
  const next = (nums.length ? Math.max(...nums) : 0) + 1;
  return `RS-${String(next).padStart(4, '0')}`;
}

function currentPublished(typeId: string): RuleSet | undefined {
  return state.ruleSets.find((r) => r.typeId === typeId && r.status === 'published' && r.isCurrent);
}

function nextVersion(typeId: string): number {
  const versions = state.ruleSets.filter((r) => r.typeId === typeId && r.version != null).map((r) => r.version as number);
  return (versions.length ? Math.max(...versions) : 0) + 1;
}

function addTimeline(task: EvaluationTask, title: string, detail?: string, actor = '系统'): EvaluationTask {
  return {
    ...task,
    updatedAt: now(),
    timeline: [
      ...task.timeline,
      { id: uid('tl'), at: now(), actor, title, detail },
    ],
  };
}

function pushWebhook(task: EvaluationTask, event: WebhookEventName, extra: Record<string, unknown> = {}) {
  if (task.isTrial) return;
  const subscribed = state.settings.subscribedEvents.includes(event);
  const log: WebhookLog = {
    id: uid('wh'),
    event,
    taskId: task.id,
    bizId: task.bizId,
    at: now(),
    payload: {
      event,
      evaluation_id: task.id,
      biz_id: task.bizId,
      status: task.status,
      type_code: task.typeCode,
      rule_set_version: task.ruleSetVersion,
      pass_mode: task.passMode,
      total_score: task.totalScore,
      passed: task.passed,
      complete: task.complete,
      ...extra,
    },
    status: !subscribed ? 'skipped' : state.settings.simulateWebhookFail ? 'failed' : 'success',
    error: !subscribed ? '未订阅该事件' : state.settings.simulateWebhookFail ? '模拟投递失败' : undefined,
    retry: state.settings.simulateWebhookFail ? 3 : 0,
  };
  state = { ...state, webhookLogs: [log, ...state.webhookLogs].slice(0, 200) };
}

function updateTask(taskId: string, updater: (t: EvaluationTask) => EvaluationTask) {
  state = {
    ...state,
    tasks: state.tasks.map((t) => (t.id === taskId ? updater(t) : t)),
  };
  emit();
}

export const api = {
  reset() {
    pipelineTokens.clear();
    state = withDemoTasks(createSeed());
    emit();
  },

  listTypes() {
    return state.types;
  },

  getType(id: string) {
    return state.types.find((t) => t.id === id);
  },

  createType(input: { name: string; description: string }) {
    const code = nextCatalogCode();
    const item: DocType = {
      id: uid('type'),
      code,
      name: input.name.trim(),
      description: (input.description || '').trim(),
      enabled: true,
      createdAt: now(),
      updatedAt: now(),
    };
    const draft: RuleSet = {
      id: uid('rs'),
      typeId: item.id,
      status: 'draft',
      isCurrent: false,
      passMode: 'veto_weighted',
      overallPassScore: 70,
      rules: [],
      createdAt: now(),
    };
    patch((s) => ({ ...s, types: [item, ...s.types], ruleSets: [draft, ...s.ruleSets] }));
    return item;
  },

  updateType(id: string, input: { name: string; description: string }) {
    patch((s) => ({
      ...s,
      types: s.types.map((t) => (t.id === id ? { ...t, name: input.name.trim(), description: input.description.trim(), updatedAt: now() } : t)),
    }));
  },

  setTypeEnabled(id: string, enabled: boolean) {
    patch((s) => ({
      ...s,
      types: s.types.map((t) => (t.id === id ? { ...t, enabled, updatedAt: now() } : t)),
    }));
  },

  listRuleSets(typeId: string) {
    return state.ruleSets
      .filter((r) => r.typeId === typeId)
      .sort((a, b) => {
        const av = a.version ?? 0;
        const bv = b.version ?? 0;
        if (a.status === 'draft' && b.status !== 'draft') return -1;
        if (b.status === 'draft' && a.status !== 'draft') return 1;
        return bv - av;
      });
  },

  getRuleSet(id: string) {
    return state.ruleSets.find((r) => r.id === id);
  },

  createDraft(typeId: string, fromId?: string) {
    const source = fromId ? state.ruleSets.find((r) => r.id === fromId) : undefined;
    const draft: RuleSet = {
      id: uid('rs'),
      typeId,
      status: 'draft',
      isCurrent: false,
      passMode: source?.passMode ?? 'veto_weighted',
      overallPassScore: source?.overallPassScore ?? 70,
      basedOnVersion: source?.version,
      rules: (source?.rules ?? []).map((r, i) => ({ ...r, id: uid('rule'), sort: i + 1 })),
      createdAt: now(),
    };
    patch((s) => ({ ...s, ruleSets: [draft, ...s.ruleSets] }));
    return draft;
  },

  updateRuleSet(id: string, patchSet: Partial<Pick<RuleSet, 'passMode' | 'overallPassScore' | 'rules'>>) {
    const rs = state.ruleSets.find((r) => r.id === id);
    if (!rs) throw new Error('规则集不存在');
    if (rs.status !== 'draft') throw new Error('已发布版本只读，请复制为新草稿');
    patch((s) => ({
      ...s,
      ruleSets: s.ruleSets.map((r) => (r.id === id ? { ...r, ...patchSet } : r)),
    }));
  },

  upsertRule(ruleSetId: string, rule: Rule) {
    const rs = state.ruleSets.find((r) => r.id === ruleSetId);
    if (!rs || rs.status !== 'draft') throw new Error('只能编辑草稿');
    const needWeight = rs.passMode !== 'all_pass';
    const err = validateRule(rule, needWeight);
    if (err) throw new Error(err);
    const exists = rs.rules.some((r) => r.id === rule.id);
    const rules = exists
      ? rs.rules.map((r) => (r.id === rule.id ? rule : r))
      : [...rs.rules, { ...rule, sort: rs.rules.length + 1 }];
    api.updateRuleSet(ruleSetId, { rules });
  },

  removeRule(ruleSetId: string, ruleId: string) {
    const rs = state.ruleSets.find((r) => r.id === ruleSetId);
    if (!rs || rs.status !== 'draft') throw new Error('只能编辑草稿');
    api.updateRuleSet(ruleSetId, {
      rules: rs.rules.filter((r) => r.id !== ruleId).map((r, i) => ({ ...r, sort: i + 1 })),
    });
  },

  moveRule(ruleSetId: string, ruleId: string, dir: -1 | 1) {
    const rs = state.ruleSets.find((r) => r.id === ruleSetId);
    if (!rs || rs.status !== 'draft') throw new Error('只能编辑草稿');
    const rules = [...rs.rules].sort((a, b) => a.sort - b.sort);
    const idx = rules.findIndex((r) => r.id === ruleId);
    const next = idx + dir;
    if (idx < 0 || next < 0 || next >= rules.length) return;
    [rules[idx], rules[next]] = [rules[next], rules[idx]];
    api.updateRuleSet(ruleSetId, { rules: rules.map((r, i) => ({ ...r, sort: i + 1 })) });
  },

  publish(ruleSetId: string) {
    const rs = state.ruleSets.find((r) => r.id === ruleSetId);
    if (!rs) throw new Error('规则集不存在');
    if (rs.status !== 'draft') throw new Error('仅草稿可发布');
    const errors = validateRuleSet(rs);
    if (errors.length) throw new Error(errors.join('；'));
    const version = nextVersion(rs.typeId);
    patch((s) => ({
      ...s,
      ruleSets: s.ruleSets.map((item) => {
        if (item.typeId === rs.typeId && item.isCurrent) {
          return { ...item, isCurrent: false, status: item.status === 'published' ? 'archived' : item.status };
        }
        if (item.id === ruleSetId) {
          return { ...item, status: 'published', isCurrent: true, version, publishedAt: now() };
        }
        return item;
      }),
    }));
    return version;
  },

  disableCurrent(typeId: string) {
    patch((s) => ({
      ...s,
      ruleSets: s.ruleSets.map((r) =>
        r.typeId === typeId && r.isCurrent ? { ...r, isCurrent: false, status: 'disabled' } : r,
      ),
    }));
  },

  listTasks(filter?: { keyword?: string; status?: string; typeId?: string; kind?: 'all' | 'trial' | 'live' }) {
    return state.tasks
      .filter((t) => {
        if (filter?.status && t.status !== filter.status) return false;
        if (filter?.typeId && t.typeId !== filter.typeId) return false;
        if (filter?.kind === 'trial' && !t.isTrial) return false;
        if (filter?.kind === 'live' && t.isTrial) return false;
        if (filter?.keyword) {
          const q = filter.keyword.toLowerCase();
          return t.id.toLowerCase().includes(q) || t.bizId.toLowerCase().includes(q) || (t.typeName ?? '').includes(filter.keyword);
        }
        return true;
      })
      .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1));
  },

  listAuditors() {
    return state.auditors;
  },

  createAuditor(input: { name: string; kind: Auditor['kind']; agentId?: string; description: string }) {
    if (!input.name.trim()) throw new Error('请填写审核器名称');
    if (input.kind === 'agent' && !input.agentId) throw new Error('Agent 审核器必须选择一个 Agent');
    const agent = state.agents.find((a) => a.id === input.agentId);
    const item: Auditor = {
      id: uid('aud'),
      name: input.name.trim(),
      kind: input.kind,
      agentId: input.kind === 'agent' ? input.agentId : undefined,
      agentName: input.kind === 'agent' ? agent?.name : undefined,
      description: (input.description || '').trim(),
      enabled: true,
      createdAt: now(),
      updatedAt: now(),
    };
    patch((s) => ({ ...s, auditors: [item, ...s.auditors] }));
    return item;
  },

  updateAuditor(id: string, input: { name: string; kind: Auditor['kind']; agentId?: string; description: string }) {
    if (input.kind === 'agent' && !input.agentId) throw new Error('Agent 审核器必须选择一个 Agent');
    const agent = state.agents.find((a) => a.id === input.agentId);
    patch((s) => ({
      ...s,
      auditors: s.auditors.map((a) =>
        a.id === id
          ? {
              ...a,
              name: input.name.trim(),
              kind: input.kind,
              agentId: input.kind === 'agent' ? input.agentId : undefined,
              agentName: input.kind === 'agent' ? agent?.name : undefined,
              description: (input.description || '').trim(),
              updatedAt: now(),
            }
          : a,
      ),
    }));
  },

  setAuditorEnabled(id: string, enabled: boolean) {
    patch((s) => ({
      ...s,
      auditors: s.auditors.map((a) => (a.id === id ? { ...a, enabled, updatedAt: now() } : a)),
    }));
  },

  getTask(id: string) {
    return state.tasks.find((t) => t.id === id);
  },

  createEvaluation(input: {
    bizId: string;
    typeCode?: string;
    auditorId?: string;
    files: { name: string; role?: string }[];
    isTrial?: boolean;
    ruleSetId?: string;
  }) {
    const bizId = input.bizId.trim();
    if (!bizId) throw new Error('业务单号必填');
    if (!input.files.length) throw new Error('至少上传 1 个附件');
    if (input.files.length > 20) throw new Error('附件数量超过 20 个上限');

    if (!input.isTrial) {
      const existed = state.tasks.find((t) => !t.isTrial && t.bizId === bizId);
      if (existed) return { task: existed, idempotent: true as const };
    }

    let type = input.typeCode ? state.types.find((t) => t.code === input.typeCode) : undefined;
    if (input.typeCode && !type) throw new Error('指定的规则集不存在');
    if (type && !type.enabled) throw new Error('该规则集已停用');

    const auditorId = input.auditorId || (input.isTrial ? 'aud-agent-quality' : undefined);
    if (!input.isTrial && !auditorId) throw new Error('请选择审核器');
    const auditor = auditorId ? state.auditors.find((a) => a.id === auditorId) : undefined;
    if (auditorId && (!auditor || !auditor.enabled)) throw new Error('审核器不可用');

    const attachments: Attachment[] = input.files.map((f, i) => ({
      id: uid('file'),
      name: f.name,
      mime: mimeOf(f.name),
      role: f.role || (i === 0 ? 'main' : 'appendix'),
      sort: i + 1,
      parseFailed: isUnparseable(f.name),
      excerpt: excerptOf(f.name),
    }));

    const task: EvaluationTask = {
      id: uid(input.isTrial ? 'TRL' : 'EVL'),
      bizId: input.isTrial ? `TRIAL-${bizId}` : bizId,
      isTrial: !!input.isTrial,
      typeId: type?.id,
      typeCode: type?.code,
      typeName: type?.name,
      typeSource: type ? 'specified' : undefined,
      auditorId: auditor?.id,
      auditorName: auditor?.name,
      auditorKind: auditor?.kind,
      agentName: auditor?.agentName,
      ruleSetId: input.ruleSetId,
      status: 'received',
      attachments,
      results: [],
      complete: false,
      annotations: [],
      timeline: [{ id: uid('tl'), at: now(), actor: input.isTrial ? '规则专家' : '接入方', title: '创建审核任务', detail: auditor ? `审核器：${auditor.name}` : undefined }],
      createdAt: now(),
      updatedAt: now(),
    };

    patch((s) => ({ ...s, tasks: [task, ...s.tasks] }));
    void runPipeline(task.id);
    return { task, idempotent: false as const };
  },

  patchScore(taskId: string, ruleId: string, score: number, reason: string) {
    const task = mustMutable(taskId);
    const result = task.results.find((r) => r.ruleId === ruleId);
    if (!result) throw new Error('规则结果不存在');
    if (result.failed) throw new Error('失败的规则没有可改的分数');
    if (score < result.minScore || score > result.maxScore) throw new Error('分数必须落在该规则区间内');
    if (!reason.trim()) throw new Error('改分原因必填');
    updateTask(taskId, (t) => {
      const next: EvaluationTask = {
        ...t,
        results: t.results.map((r) =>
          r.ruleId === ruleId ? { ...r, humanScore: score, humanReason: reason.trim() } : r,
        ),
      };
      const summarized = addTimeline(applySummary(next), '人工改分', `${result.ruleName}：${displayScore(result) ?? '-'} → ${score}。${reason}`, '接入方');
      pushWebhook(summarized, 'evaluation.updated', { rule_id: ruleId, human_score: score });
      return summarized;
    });
  },

  addAnnotation(taskId: string, input: Omit<Annotation, 'id' | 'createdAt' | 'actor'>) {
    mustMutable(taskId);
    if (!input.content.trim()) throw new Error('标注内容不能为空');
    const annotation: Annotation = {
      ...input,
      id: uid('an'),
      actor: '接入方',
      createdAt: now(),
      content: input.content.trim(),
    };
    updateTask(taskId, (t) => {
      const next = addTimeline({ ...t, annotations: [...t.annotations, annotation] }, '新增标注', annotation.content, '接入方');
      pushWebhook(next, 'evaluation.updated', { annotation_id: annotation.id });
      return next;
    });
    return annotation;
  },

  finalize(taskId: string) {
    const task = mustMutable(taskId);
    if (task.status !== 'scored') throw new Error('仅机评完成的任务可锁定');
    updateTask(taskId, (t) => {
      const next = addTimeline({ ...t, status: 'finalized' }, '锁定终态', '结果只读', '接入方');
      pushWebhook(next, 'evaluation.finalized', { passed: next.passed });
      return next;
    });
  },

  reclassify(taskId: string, typeCode: string) {
    const task = state.tasks.find((t) => t.id === taskId);
    if (!task) throw new Error('任务不存在');
    if (task.status === 'finalized') throw new Error('已锁定，不可重评');
    if (task.status !== 'type_pending' && task.status !== 'scored') {
      throw new Error('当前状态不可指定规则集重评');
    }
    const type = state.types.find((t) => t.code === typeCode);
    if (!type || !type.enabled) throw new Error('规则集不可用');
    updateTask(taskId, (t) =>
      addTimeline(
        {
          ...t,
          typeId: type.id,
          typeCode: type.code,
          typeName: type.name,
          typeSource: 'specified',
          results: [],
          passed: undefined,
          totalScore: undefined,
          complete: false,
          annotations: [],
          status: 'received',
        },
        '指定规则集并重评',
        `规则集改为 ${type.name}，机评结果与人工分将覆盖`,
        '接入方',
      ),
    );
    void runPipeline(taskId);
  },

  createCredential(name: string) {
    const secret = `sk_live_${Math.random().toString(36).slice(2, 14)}`;
    const item: Credential = {
      id: uid('cred'),
      name: name.trim() || '未命名接入方',
      keyPrefix: `ak_live_${Math.random().toString(36).slice(2, 6)}`,
      lastSecret: secret,
      enabled: true,
      createdAt: now(),
    };
    patch((s) => ({ ...s, credentials: [item, ...s.credentials] }));
    return item;
  },

  clearCredentialSecret(id: string) {
    patch((s) => ({
      ...s,
      credentials: s.credentials.map((c) => (c.id === id ? { ...c, lastSecret: undefined } : c)),
    }));
  },

  setCredentialEnabled(id: string, enabled: boolean) {
    patch((s) => ({
      ...s,
      credentials: s.credentials.map((c) => (c.id === id ? { ...c, enabled } : c)),
    }));
  },

  updateSettings(partial: Partial<AppState['settings']>) {
    patch((s) => ({ ...s, settings: { ...s.settings, ...partial } }));
  },

  retryWebhook(id: string) {
    patch((s) => ({
      ...s,
      webhookLogs: s.webhookLogs.map((w) =>
        w.id === id ? { ...w, status: 'success', error: undefined, retry: w.retry + 1, at: now() } : w,
      ),
    }));
  },
};

function mustMutable(taskId: string): EvaluationTask {
  const task = state.tasks.find((t) => t.id === taskId);
  if (!task) throw new Error('任务不存在');
  if (task.status === 'finalized') throw new Error('已锁定，不可修改');
  if (task.status !== 'scored') throw new Error('仅机评完成后可改分或标注');
  return task;
}

async function runPipeline(taskId: string) {
  const token = (pipelineTokens.get(taskId) ?? 0) + 1;
  pipelineTokens.set(taskId, token);
  const alive = () => pipelineTokens.get(taskId) === token;

  updateTask(taskId, (t) => addTimeline({ ...t, status: 'parsing' }, '开始解析内容包'));
  await delay(500);
  if (!alive()) return;

  const current = state.tasks.find((t) => t.id === taskId);
  if (!current) return;

  const attachments = current.attachments.map((a) => ({ ...a, parseFailed: isUnparseable(a.name) }));
  if (attachments.every((a) => a.parseFailed)) {
    updateTask(taskId, (t) => {
      const failed = addTimeline({ ...t, attachments, status: 'failed', failReason: '全部附件解析失败' }, '解析失败');
      pushWebhook(failed, 'evaluation.failed', { reason: failed.failReason });
      return failed;
    });
    return;
  }

  updateTask(taskId, (t) => addTimeline({ ...t, attachments }, '解析完成', attachments.filter((a) => a.parseFailed).length ? '部分附件解析失败，已用其余文件继续' : undefined));

  let typeId = current.typeId;
  let typeCode = current.typeCode;
  let typeName = current.typeName;
  let typeSource = current.typeSource;
  let classifyConfidence = current.classifyConfidence;
  let classifyReason = current.classifyReason;

  if (!typeCode) {
    updateTask(taskId, (t) => addTimeline({ ...t, status: 'classifying' }, '未指定规则集，开始自动匹配'));
    await delay(450);
    if (!alive()) return;
    const classified = classifyFromFiles(attachments);
    classifyConfidence = classified.confidence;
    classifyReason = classified.reason;
    const hit = classified.code ? state.types.find((t) => t.code === classified.code) : undefined;
    if (!hit || classified.confidence < state.settings.classifyThreshold) {
      updateTask(taskId, (t) => {
        const pending = addTimeline(
          {
            ...t,
            status: 'type_pending',
            typeCode: hit?.code,
            typeName: hit?.name,
            typeId: hit?.id,
            typeSource: 'classified',
            classifyConfidence,
            classifyReason,
          },
          '识别置信度不足，停止打分',
          `候选 ${hit?.name ?? '无'}（${Math.round((classified.confidence ?? 0) * 100)}%）`,
        );
        pushWebhook(pending, 'evaluation.classified', { pending: true, confidence: classified.confidence });
        return pending;
      });
      return;
    }
    typeId = hit.id;
    typeCode = hit.code;
    typeName = hit.name;
    typeSource = 'classified';
    updateTask(taskId, (t) => {
      const next = addTimeline(
        { ...t, typeId, typeCode, typeName, typeSource, classifyConfidence, classifyReason },
        '识别成功',
        `${typeName}（${Math.round(classified.confidence * 100)}%）`,
      );
      pushWebhook(next, 'evaluation.classified', { pending: false, confidence: classified.confidence });
      return next;
    });
  }

  const type = state.types.find((t) => t.id === typeId);
  if (!type?.enabled) {
    failTask(taskId, '规则集已停用');
    return;
  }

  let ruleSet = current.isTrial && current.ruleSetId
    ? state.ruleSets.find((r) => r.id === current.ruleSetId)
    : currentPublished(typeId!);
  if (!ruleSet) {
    failTask(taskId, '该规则集没有当前已发布版本');
    return;
  }

  const auditor = current.auditorId
    ? state.auditors.find((a) => a.id === current.auditorId)
    : undefined;
  const scoreMode = auditor?.kind === 'ordinary' ? 'ordinary' : 'agent';

  updateTask(taskId, (t) =>
    addTimeline(
      {
        ...t,
        status: 'scoring',
        typeId,
        typeCode,
        typeName,
        typeSource,
        classifyConfidence,
        classifyReason,
        ruleSetId: ruleSet!.id,
        ruleSetVersion: ruleSet!.version ?? (ruleSet!.status === 'draft' ? undefined : ruleSet!.version),
        passMode: ruleSet!.passMode,
        overallPassScore: ruleSet!.overallPassScore,
      },
      '加载规则集',
      `${ruleSet!.status === 'draft' ? '试评绑定草稿快照' : `v${ruleSet!.version}`}${auditor ? ` · ${auditor.name}` : ''}`,
    ),
  );

  const rules = [...ruleSet.rules].sort((a, b) => a.sort - b.sort);
  for (let i = 0; i < rules.length; i += 1) {
    await delay(280);
    if (!alive()) return;
    updateTask(taskId, (t) => ({
      ...t,
      results: snapshotResults(rules.slice(0, i + 1), attachments, scoreMode).map((row, idx) => {
        const prev = t.results[idx];
        return prev && prev.ruleId === row.ruleId && prev.humanScore != null ? prev : row;
      }),
    }));
  }

  if (!alive()) return;
  const latest = state.tasks.find((t) => t.id === taskId);
  if (!latest) return;
  const full = snapshotResults(rules, attachments, scoreMode);
  if (full.length > 0 && full.every((r) => r.failed)) {
    failTask(taskId, '全部规则机评失败');
    return;
  }

  updateTask(taskId, (t) => {
    const scored = applySummary(
      addTimeline(
        {
          ...t,
          status: 'scored',
          results: full,
        },
        '机评完成',
        auditor?.kind === 'ordinary' ? '普通审核器基线分' : auditor?.agentName ? `Agent：${auditor.agentName}` : undefined,
      ),
    );
    pushWebhook(scored, 'evaluation.scored', {
      passed: scored.passed,
      total_score: scored.totalScore,
    });
    return scored;
  });
}

function failTask(taskId: string, reason: string) {
  updateTask(taskId, (t) => {
    const failed = addTimeline({ ...t, status: 'failed', failReason: reason }, '任务失败', reason);
    pushWebhook(failed, 'evaluation.failed', { reason });
    return failed;
  });
}

function mimeOf(name: string) {
  if (/\.pdf$/i.test(name)) return 'application/pdf';
  if (/\.docx?$/i.test(name)) return 'application/msword';
  if (/\.xlsx?$/i.test(name)) return 'application/vnd.ms-excel';
  return 'application/octet-stream';
}

function excerptOf(name: string) {
  if (/缺风险|无应对/i.test(name)) return '项目存在市场与技术风险，但未给出应对措施与责任人。';
  if (/优秀|完整/i.test(name)) return '已列明市场/技术/财务风险，并附应对计划与里程碑。';
  if (/季报/i.test(name)) return '本季度目标完成率 86%，未完成项已说明原因。';
  if (/尽调/i.test(name)) return '目标公司股权结构清晰，近三年无重大诉讼记录。';
  return '报告提出建设智能评审能力，计划于 Q4 完成试点并覆盖 3 条业务线。';
}

function withDemoTasks(seed: AppState): AppState {
  const published = seed.ruleSets.find((r) => r.id === 'rs-proposal-v3')!;
  const filesOk: Attachment[] = [
    { id: 'f-main', name: '立项报告-缺风险.pdf', mime: 'application/pdf', role: 'main', sort: 1, parseFailed: false, excerpt: excerptOf('立项报告-缺风险.pdf') },
    { id: 'f-fin', name: '测算表.xlsx', mime: 'application/vnd.ms-excel', role: 'financial', sort: 2, parseFailed: false, excerpt: excerptOf('测算表.xlsx') },
  ];
  const filesGood: Attachment[] = [
    { id: 'f-good', name: '立项报告-完整版.pdf', mime: 'application/pdf', role: 'main', sort: 1, parseFailed: false, excerpt: excerptOf('立项报告-完整版.pdf') },
  ];

  const scoredRaw: EvaluationTask = applySummary({
    id: 'EVL-demo-scored',
    bizId: 'APP-8891',
    isTrial: false,
    typeId: 't-proposal',
    typeCode: 'RS-0001',
    typeName: '立项报告规则集',
    typeSource: 'specified',
    auditorId: 'aud-agent-quality',
    auditorName: '立项质量审核器',
    auditorKind: 'agent',
    agentName: '文档质量审核 Agent',
    ruleSetId: published.id,
    ruleSetVersion: 3,
    passMode: published.passMode,
    overallPassScore: published.overallPassScore,
    status: 'scored',
    attachments: filesOk,
    results: snapshotResults(published.rules, filesOk),
    complete: true,
    annotations: [],
    timeline: [
      { id: 'tl1', at: '2026-08-29 09:10:00', actor: '接入方', title: '创建评审任务' },
      { id: 'tl2', at: '2026-08-29 09:10:02', actor: '系统', title: '解析完成' },
      { id: 'tl3', at: '2026-08-29 09:10:08', actor: '系统', title: '机评完成' },
    ],
    createdAt: '2026-08-29 09:10:00',
    updatedAt: '2026-08-29 09:10:08',
  });

  const human = applySummary({
    ...scoredRaw,
    id: 'EVL-demo-human',
    bizId: 'APP-9002',
    results: scoredRaw.results.map((r) =>
      r.ruleId === 'r-risk'
        ? { ...r, humanScore: 7, humanReason: '附件测算表补充了风险应对措施。' }
        : r,
    ),
    annotations: [
      {
        id: 'an1',
        target: 'file',
        fileId: 'f-fin',
        location: 'Sheet1',
        content: '测算表第 3 栏已列出应对预算。',
        actor: '接入方',
        createdAt: '2026-08-29 11:20:00',
      },
    ],
    status: 'finalized',
    timeline: [
      ...scoredRaw.timeline,
      { id: 'tl4', at: '2026-08-29 11:20:00', actor: '接入方', title: '人工改分', detail: '风险披露：4 → 7' },
      { id: 'tl5', at: '2026-08-29 11:21:00', actor: '接入方', title: '锁定终态' },
    ],
    createdAt: '2026-08-29 09:40:00',
    updatedAt: '2026-08-29 11:21:00',
  });

  const pending: EvaluationTask = {
    id: 'EVL-demo-pending',
    bizId: 'MAIL-331',
    isTrial: false,
    typeCode: 'RS-0001',
    typeName: '立项报告规则集',
    typeId: 't-proposal',
    typeSource: 'classified',
    classifyConfidence: 0.41,
    classifyReason: '文件名含糊，正文特征不足，无法稳定匹配规则集。',
    auditorId: 'aud-agent-quality',
    auditorName: '立项质量审核器',
    auditorKind: 'agent',
    agentName: '文档质量审核 Agent',
    status: 'type_pending',
    attachments: [
      { id: 'f-unk', name: '未知材料.pdf', mime: 'application/pdf', role: 'main', sort: 1, parseFailed: false, excerpt: excerptOf('未知材料.pdf') },
    ],
    results: [],
    complete: false,
    annotations: [],
    timeline: [
      { id: 'tlp1', at: '2026-08-29 15:00:00', actor: '接入方', title: '创建评审任务' },
      { id: 'tlp2', at: '2026-08-29 15:00:03', actor: '系统', title: '识别置信度不足，停止打分' },
    ],
    createdAt: '2026-08-29 15:00:00',
    updatedAt: '2026-08-29 15:00:03',
  };

  const failed: EvaluationTask = {
    id: 'EVL-demo-failed',
    bizId: 'APP-Q-01',
    isTrial: false,
    typeId: 't-qreport',
    typeCode: 'RS-0002',
    typeName: '季度工作报告规则集',
    typeSource: 'specified',
    auditorId: 'aud-ordinary',
    auditorName: '结构化基线审核器',
    auditorKind: 'ordinary',
    status: 'failed',
    failReason: '该规则集没有当前已发布版本',
    attachments: [
      { id: 'f-q', name: '三季度工作报告.docx', mime: 'application/msword', role: 'main', sort: 1, parseFailed: false, excerpt: excerptOf('季报.docx') },
    ],
    results: [],
    complete: false,
    annotations: [],
    timeline: [
      { id: 'tlf1', at: '2026-08-29 16:00:00', actor: '接入方', title: '创建评审任务' },
      { id: 'tlf2', at: '2026-08-29 16:00:01', actor: '系统', title: '任务失败', detail: '该规则集没有当前已发布版本' },
    ],
    createdAt: '2026-08-29 16:00:00',
    updatedAt: '2026-08-29 16:00:01',
  };

  const trial: EvaluationTask = applySummary({
    id: 'TRL-demo',
    bizId: 'TRIAL-draft-1',
    isTrial: true,
    typeId: 't-proposal',
    typeCode: 'RS-0001',
    typeName: '立项报告规则集',
    typeSource: 'specified',
    auditorId: 'aud-agent-quality',
    auditorName: '立项质量审核器',
    auditorKind: 'agent',
    agentName: '文档质量审核 Agent',
    ruleSetId: 'rs-proposal-draft',
    passMode: 'veto_weighted',
    overallPassScore: 72,
    status: 'scored',
    attachments: filesGood,
    results: snapshotResults(seed.ruleSets.find((r) => r.id === 'rs-proposal-draft')!.rules, filesGood),
    complete: true,
    annotations: [],
    timeline: [{ id: 'tlt', at: '2026-08-29 14:10:00', actor: '规则专家', title: '样例试评' }],
    createdAt: '2026-08-29 14:10:00',
    updatedAt: '2026-08-29 14:10:06',
  });

  const webhookFailLog: WebhookLog = {
    id: 'wh-fail-1',
    event: 'evaluation.scored',
    taskId: scoredRaw.id,
    bizId: scoredRaw.bizId,
    at: '2026-08-29 09:10:08',
    payload: { evaluation_id: scoredRaw.id, status: 'scored', passed: scoredRaw.passed },
    status: 'failed',
    error: '接入方回调 502',
    retry: 3,
  };

  return {
    ...seed,
    tasks: [scoredRaw, human, pending, failed, trial],
    webhookLogs: [
      webhookFailLog,
      {
        id: 'wh-ok-1',
        event: 'evaluation.finalized',
        taskId: human.id,
        bizId: human.bizId,
        at: '2026-08-29 11:21:00',
        payload: { evaluation_id: human.id, status: 'finalized', passed: human.passed },
        status: 'success',
        retry: 0,
      },
    ],
  };
}

export { PASS_MODE_LABEL, PASS_MODE_HINT } from './engine';
