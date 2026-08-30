import type {
  AgentOption,
  Annotation,
  Attachment,
  Auditor,
  AuditorKind,
  Credential,
  DocType,
  EvaluationTask,
  Evidence,
  IntegrationSettings,
  PassMode,
  Rule,
  RuleEngineKind,
  RuleResult,
  RuleSet,
  RuleSetStatus,
  Scene,
  SceneParam,
  TaskStatus,
  TimelineEvent,
  TypeSource,
  WebhookEventName,
  WebhookLog,
} from '../types/models';
import type {
  AgentOptionDTO,
  AnnotationDTO,
  AttachmentDTO,
  AuditorDTO,
  CredentialDTO,
  EvaluationDetailDTO,
  EvaluationListDTO,
  IntegrationDTO,
  RuleItemDTO,
  RuleResultDTO,
  RuleSetDTO,
  RuleSetDetailDTO,
  RuleSetVersionDTO,
  SceneDTO,
  SceneParamDTO,
  TimelineDTO,
  WebhookLogDTO,
} from './dto';

const SCORE_MODE_TO_UI: Record<string, PassMode> = {
  ALL_PASS: 'all_pass',
  WEIGHTED_SUM: 'weighted_sum',
  VETO_WEIGHTED: 'veto_weighted',
  all_pass: 'all_pass',
  weighted_sum: 'weighted_sum',
  veto_weighted: 'veto_weighted',
};

export const UI_TO_SCORE_MODE: Record<PassMode, string> = {
  all_pass: 'ALL_PASS',
  weighted_sum: 'WEIGHTED_SUM',
  veto_weighted: 'VETO_WEIGHTED',
};

const VERSION_STATUS_TO_UI: Record<string, RuleSetStatus> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  ARCHIVED: 'archived',
  DISABLED: 'disabled',
  draft: 'draft',
  published: 'published',
  archived: 'archived',
  disabled: 'disabled',
};

const TASK_STATUS_TO_UI: Record<string, TaskStatus> = {
  RECEIVED: 'received',
  PARSING: 'parsing',
  CLASSIFYING: 'classifying',
  SCORING: 'scoring',
  TYPE_PENDING: 'type_pending',
  SCORED: 'scored',
  FINALIZED: 'finalized',
  FAILED: 'failed',
  received: 'received',
  parsing: 'parsing',
  classifying: 'classifying',
  scoring: 'scoring',
  type_pending: 'type_pending',
  scored: 'scored',
  finalized: 'finalized',
  failed: 'failed',
};

const KIND_TO_UI: Record<string, AuditorKind> = {
  AGENT: 'agent',
  ORDINARY: 'ordinary',
  agent: 'agent',
  ordinary: 'ordinary',
};

export const UI_TO_KIND: Record<AuditorKind, string> = {
  agent: 'AGENT',
  ordinary: 'ORDINARY',
};

const SOURCE_TO_UI: Record<string, TypeSource> = {
  SPECIFIED: 'specified',
  CLASSIFIED: 'classified',
  specified: 'specified',
  classified: 'classified',
};

export const TERMINAL_TASK_STATUSES = new Set(['SCORED', 'FAILED', 'TYPE_PENDING']);

export function toTaskStatus(raw?: string): TaskStatus {
  if (!raw) return 'received';
  return TASK_STATUS_TO_UI[raw] ?? TASK_STATUS_TO_UI[raw.toUpperCase()] ?? 'received';
}

export function toBackendStatus(status?: string) {
  if (!status) return undefined;
  return status.toUpperCase();
}

export function toPassMode(raw?: string): PassMode {
  if (!raw) return 'veto_weighted';
  return SCORE_MODE_TO_UI[raw] ?? 'veto_weighted';
}

export const UI_TO_ENGINE: Record<RuleEngineKind, string> = {
  ordinary: 'ORDINARY',
  agent: 'AGENT',
};

const ENGINE_TO_UI: Record<string, RuleEngineKind> = {
  ORDINARY: 'ordinary',
  AGENT: 'agent',
  ordinary: 'ordinary',
  agent: 'agent',
};

export function toSceneParam(dto: SceneParamDTO): SceneParam {
  return {
    key: dto.key,
    label: dto.label ?? dto.key,
    type: dto.type ?? 'STRING',
    builtin: !!dto.builtin,
  };
}

export function toScene(dto: SceneDTO): Scene {
  return {
    id: dto.num,
    name: dto.name,
    description: dto.description ?? '',
    extraParams: (dto.extraParams ?? []).map(toSceneParam),
    params: (dto.params ?? []).map(toSceneParam),
    enabled: dto.enabled !== false,
    updatedAt: String(dto.updateTime ?? ''),
  };
}

export function toDocType(dto: RuleSetDTO | RuleSetDetailDTO): DocType {
  const detail = dto as RuleSetDetailDTO;
  return {
    id: dto.num,
    code: dto.num,
    name: dto.name,
    description: dto.description ?? '',
    sceneNum: dto.sceneNum,
    sceneName: dto.sceneName,
    params: (detail.sceneParams ?? []).map(toSceneParam),
    enabled: dto.enabled !== false,
    currentVersionNo: dto.currentVersionNo,
    passMode: dto.scoreMode ? toPassMode(dto.scoreMode) : 'veto_weighted',
    overallPassScore: Number(dto.overallPassScore ?? 70),
    createdAt: 'createTime' in dto ? String(dto.createTime ?? '') : '',
    updatedAt: String(dto.updateTime ?? ''),
  };
}

export function toRule(dto: RuleItemDTO): Rule {
  return {
    id: dto.num,
    name: dto.name,
    standard: dto.standard,
    minScore: Number(dto.minScore ?? 0),
    maxScore: Number(dto.maxScore ?? 10),
    passScore: Number(dto.passScore ?? 6),
    weight: Number(dto.weight ?? 1),
    isVeto: !!dto.veto,
    positiveExample: dto.positiveExample ?? '',
    negativeExample: dto.negativeExample ?? '',
    sort: dto.sortNo ?? 1,
    engineKind: ENGINE_TO_UI[dto.engineKind ?? ''] ?? 'ordinary',
    auditorId: dto.auditorNum ?? '',
    auditorName: dto.auditorName,
    checks: (dto.checks ?? []).map((item) => ({
      paramKey: item.paramKey,
      op: item.op,
      value: item.value,
    })),
    agentParamKeys: dto.agentParamKeys ?? [],
  };
}

export function toRuleSet(dto: RuleSetVersionDTO): RuleSet {
  return {
    id: dto.num,
    typeId: dto.ruleSetNum,
    version: dto.versionNo,
    status: VERSION_STATUS_TO_UI[dto.status ?? ''] ?? 'draft',
    isCurrent: !!dto.currentFlag,
    passMode: toPassMode(dto.scoreMode),
    overallPassScore: Number(dto.overallPassScore ?? 0),
    rules: (dto.rules ?? []).map(toRule),
    basedOnVersion: dto.basedOnVersionNo,
    createdAt: dto.createTime ?? '',
  };
}

export function toAgent(dto: AgentOptionDTO): AgentOption {
  return {
    id: dto.agentNum,
    name: dto.name,
    description: dto.description ?? '',
    provider: dto.provider ?? '',
  };
}

export function toAuditor(dto: AuditorDTO): Auditor {
  const kind = KIND_TO_UI[dto.kind ?? ''] ?? 'ordinary';
  return {
    id: dto.num,
    name: dto.name,
    kind,
    agentId: dto.agentNum,
    agentName: dto.agentName,
    description: dto.description ?? '',
    enabled: dto.enabled !== false,
    createdAt: '',
    updatedAt: '',
  };
}

function parseEvidence(raw?: string): Evidence[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    const list = Array.isArray(parsed) ? parsed : [];
    return list.map((item) => {
      const ev = item as Partial<Evidence> & { fileNum?: string; name?: string };
      return {
        fileId: ev.fileId ?? ev.fileNum ?? '',
        fileName: ev.fileName ?? ev.name ?? '',
        location: ev.location,
        quote: ev.quote ?? '',
        locationFound: ev.locationFound !== false,
      };
    });
  } catch {
    return [];
  }
}

export function toAttachment(dto: AttachmentDTO): Attachment {
  return {
    id: dto.num,
    name: dto.fileName ?? dto.num,
    mime: dto.mime ?? '',
    role: dto.role ?? 'appendix',
    sort: dto.sortNo ?? 1,
    parseFailed: !!dto.parseFailed,
    excerpt: dto.excerpt ?? '',
  };
}

export function toRuleResult(dto: RuleResultDTO): RuleResult {
  return {
    ruleId: dto.ruleNum,
    ruleName: dto.ruleName ?? '',
    standard: dto.standard ?? '',
    minScore: Number(dto.minScore ?? 0),
    maxScore: Number(dto.maxScore ?? 10),
    passScore: Number(dto.passScore ?? 0),
    weight: Number(dto.weight ?? 0),
    isVeto: !!dto.veto,
    machineScore: dto.machineScore == null ? undefined : Number(dto.machineScore),
    machineRationale: dto.machineRationale,
    evidence: parseEvidence(dto.evidenceJson),
    humanScore: dto.humanScore == null ? undefined : Number(dto.humanScore),
    humanReason: dto.humanReason,
    failed: !!dto.failed,
    failReason: dto.failReason,
  };
}

export function toAnnotation(dto: AnnotationDTO): Annotation {
  return {
    id: dto.num,
    target: dto.target === 'file' ? 'file' : 'rule',
    ruleId: dto.ruleNum,
    fileId: dto.fileNum,
    location: dto.location,
    content: dto.content ?? '',
    actor: '',
    createdAt: '',
  };
}

export function toTimeline(dto: TimelineDTO): TimelineEvent {
  return {
    id: dto.num,
    at: dto.createTime ?? '',
    actor: dto.actor ?? '',
    title: dto.title ?? '',
    detail: dto.detail,
  };
}

export function toTask(dto: EvaluationListDTO, names?: { ruleSetName?: string; auditorName?: string }): EvaluationTask {
  return {
    id: dto.num,
    bizId: dto.bizId,
    isTrial: !!dto.trial,
    typeId: dto.ruleSetNum,
    typeCode: dto.ruleSetNum,
    typeName: names?.ruleSetName,
    auditorId: dto.auditorNum,
    auditorName: names?.auditorName,
    auditorKind: dto.auditorKind ? KIND_TO_UI[dto.auditorKind] : undefined,
    ruleSetVersion: dto.ruleSetVersionNo,
    status: toTaskStatus(dto.status),
    attachments: [],
    results: [],
    totalScore: dto.totalScore == null ? undefined : Number(dto.totalScore),
    passed: dto.passed,
    complete: dto.complete !== false,
    annotations: [],
    timeline: [],
    createdAt: dto.createTime ?? '',
    updatedAt: dto.createTime ?? '',
  };
}

export function toTaskDetail(dto: EvaluationDetailDTO, names?: { ruleSetName?: string; auditorName?: string }): EvaluationTask {
  return {
    id: dto.num,
    bizId: dto.bizId,
    isTrial: !!dto.trial,
    typeId: dto.ruleSetNum,
    typeCode: dto.ruleSetNum,
    typeName: names?.ruleSetName,
    typeSource: dto.ruleSetSource ? SOURCE_TO_UI[dto.ruleSetSource] : undefined,
    auditorId: dto.auditorNum,
    auditorName: names?.auditorName,
    auditorKind: dto.auditorKind ? KIND_TO_UI[dto.auditorKind] : undefined,
    agentName: dto.agentName,
    classifyConfidence: dto.classifyConfidence == null ? undefined : Number(dto.classifyConfidence),
    classifyReason: dto.classifyReason,
    ruleSetId: dto.ruleSetVersionNum,
    ruleSetVersion: dto.ruleSetVersionNo,
    passMode: dto.scoreMode ? toPassMode(dto.scoreMode) : undefined,
    overallPassScore: dto.overallPassScore == null ? undefined : Number(dto.overallPassScore),
    status: toTaskStatus(dto.status),
    failReason: dto.failReason,
    attachments: (dto.attachments ?? []).map(toAttachment),
    results: (dto.results ?? []).map(toRuleResult),
    totalScore: dto.totalScore == null ? undefined : Number(dto.totalScore),
    passed: dto.passed,
    complete: dto.complete !== false,
    annotations: (dto.annotations ?? []).map(toAnnotation),
    timeline: (dto.timeline ?? []).map(toTimeline),
    createdAt: dto.createTime ?? '',
    updatedAt: dto.updateTime ?? '',
  };
}

export function toCredential(dto: CredentialDTO): Credential {
  return {
    id: dto.num,
    name: dto.name,
    keyPrefix: dto.keyPrefix ?? '',
    enabled: dto.enabled !== false,
    createdAt: dto.createTime ?? '',
  };
}

export function toIntegration(dto: IntegrationDTO): IntegrationSettings {
  const events = (dto.subscribedEvents ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean) as WebhookEventName[];
  return {
    callbackUrl: dto.callbackUrl ?? '',
    subscribedEvents: events,
    simulateWebhookFail: false,
    classifyThreshold: Number(dto.classifyThreshold ?? 0.6),
  };
}

export function toWebhookLog(dto: WebhookLogDTO): WebhookLog {
  const statusRaw = (dto.status ?? '').toLowerCase();
  const status: WebhookLog['status'] =
    statusRaw === 'success' || statusRaw === 'failed' || statusRaw === 'skipped' ? statusRaw : 'failed';
  return {
    id: dto.num,
    event: (dto.eventName as WebhookEventName) || 'evaluation.scored',
    taskId: dto.evaluationNum ?? '',
    bizId: dto.bizId ?? '',
    at: dto.createTime ?? '',
    payload: {},
    status,
    error: dto.lastError,
    retry: dto.retryCount ?? 0,
  };
}

export function mimeOf(name: string) {
  if (/\.pdf$/i.test(name)) return 'application/pdf';
  if (/\.docx?$/i.test(name)) return 'application/msword';
  if (/\.xlsx?$/i.test(name)) return 'application/vnd.ms-excel';
  return 'application/octet-stream';
}
