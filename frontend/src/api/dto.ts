export type PageDTO<T> = {
  total: number;
  pageNo: number;
  pageSize: number;
  list: T[];
};

export function asPage<T>(raw: Partial<PageDTO<T>> | T[] | undefined | null): PageDTO<T> {
  if (Array.isArray(raw)) {
    return { total: raw.length, pageNo: 1, pageSize: raw.length, list: raw };
  }
  return {
    total: raw?.total ?? 0,
    pageNo: raw?.pageNo ?? 1,
    pageSize: raw?.pageSize ?? 20,
    list: raw?.list ?? [],
  };
}

export type SceneParamDTO = {
  key: string;
  label?: string;
  type?: string;
  builtin?: boolean;
};

export type SceneDTO = {
  num: string;
  name: string;
  description?: string;
  enabled?: boolean;
  extraParams?: SceneParamDTO[];
  params?: SceneParamDTO[];
  updateTime?: string;
};

export type RuleSetDTO = {
  num: string;
  name: string;
  description?: string;
  sceneNum?: string;
  sceneName?: string;
  enabled?: boolean;
  currentPublishedVersionNum?: string;
  currentVersionNo?: number;
  ruleCount?: number;
  scoreMode?: string;
  overallPassScore?: number;
  updateTime?: string;
};

export type RuleCheckDTO = {
  paramKey: string;
  op: string;
  value?: string;
};

export type RuleItemDTO = {
  num: string;
  name: string;
  standard: string;
  minScore?: number;
  maxScore?: number;
  passScore?: number;
  weight?: number;
  veto?: boolean;
  positiveExample?: string;
  negativeExample?: string;
  sortNo?: number;
  engineKind?: string;
  auditorNum?: string;
  auditorName?: string;
  checks?: RuleCheckDTO[];
  agentParamKeys?: string[];
};

export type RuleSetVersionDTO = {
  num: string;
  ruleSetNum: string;
  versionNo?: number;
  status?: string;
  currentFlag?: boolean;
  scoreMode?: string;
  overallPassScore?: number;
  basedOnVersionNo?: number;
  createTime?: string;
  rules?: RuleItemDTO[];
};

export type RuleSetDetailDTO = {
  num: string;
  name: string;
  description?: string;
  sceneNum?: string;
  sceneName?: string;
  sceneParams?: SceneParamDTO[];
  enabled?: boolean;
  scoreMode?: string;
  overallPassScore?: number;
  currentPublishedVersionNum?: string;
  currentVersionNo?: number;
  createTime?: string;
  updateTime?: string;
  versions?: RuleSetVersionDTO[];
};

export type AuditorDTO = {
  num: string;
  name: string;
  kind?: string;
  agentNum?: string;
  agentName?: string;
  description?: string;
  enabled?: boolean;
};

export type AgentOptionDTO = {
  agentNum: string;
  name: string;
  description?: string;
  provider?: string;
};

export type EvaluationListDTO = {
  num: string;
  bizId: string;
  trial?: boolean;
  status?: string;
  auditorNum?: string;
  auditorKind?: string;
  ruleSetNum?: string;
  ruleSetVersionNo?: number;
  totalScore?: number;
  passed?: boolean;
  complete?: boolean;
  createTime?: string;
};

export type AttachmentDTO = {
  num: string;
  objectKey?: string;
  fileName?: string;
  mime?: string;
  role?: string;
  sortNo?: number;
  parseFailed?: boolean;
  excerpt?: string;
};

export type RuleResultDTO = {
  num?: string;
  ruleNum: string;
  ruleName?: string;
  standard?: string;
  minScore?: number;
  maxScore?: number;
  passScore?: number;
  weight?: number;
  veto?: boolean;
  machineScore?: number;
  machineRationale?: string;
  humanScore?: number;
  humanReason?: string;
  displayScore?: number;
  failed?: boolean;
  failReason?: string;
  evidenceJson?: string;
};

export type AnnotationDTO = {
  num: string;
  target?: string;
  ruleNum?: string;
  fileNum?: string;
  location?: string;
  content?: string;
};

export type TimelineDTO = {
  num: string;
  actor?: string;
  title?: string;
  detail?: string;
  createTime?: string;
};

export type EvaluationDetailDTO = {
  num: string;
  bizId: string;
  trial?: boolean;
  status?: string;
  auditorNum?: string;
  auditorKind?: string;
  agentName?: string;
  ruleSetNum?: string;
  ruleSetVersionNum?: string;
  ruleSetVersionNo?: number;
  ruleSetSource?: string;
  classifyConfidence?: number;
  classifyReason?: string;
  scoreMode?: string;
  overallPassScore?: number;
  totalScore?: number;
  passed?: boolean;
  complete?: boolean;
  failReason?: string;
  callbackUrl?: string;
  createTime?: string;
  updateTime?: string;
  attachments?: AttachmentDTO[];
  results?: RuleResultDTO[];
  annotations?: AnnotationDTO[];
  timeline?: TimelineDTO[];
};

export type EvaluationCreatedDTO = {
  num: string;
  idempotent?: boolean;
};

export type AttachmentParam = {
  objectKey?: string;
  fileUrl?: string;
  fileName: string;
  mime?: string;
  role?: string;
};

export type CredentialDTO = {
  num: string;
  name: string;
  keyPrefix?: string;
  enabled?: boolean;
  createTime?: string;
};

export type CredentialSecretDTO = {
  num: string;
  name: string;
  keyPrefix?: string;
  rawSecret?: string;
};

export type IntegrationDTO = {
  num?: string;
  callbackUrl?: string;
  subscribedEvents?: string;
  classifyThreshold?: number;
};

export type WebhookLogDTO = {
  num: string;
  eventId?: string;
  evaluationNum?: string;
  bizId?: string;
  eventName?: string;
  status?: string;
  retryCount?: number;
  nextRetryTime?: string;
  lastError?: string;
  createTime?: string;
};
