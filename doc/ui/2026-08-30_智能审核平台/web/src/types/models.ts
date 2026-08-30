export type PassMode = 'all_pass' | 'weighted_sum' | 'veto_weighted';

export type RuleSetStatus = 'draft' | 'published' | 'archived' | 'disabled';

export type TaskStatus =
  | 'received'
  | 'parsing'
  | 'classifying'
  | 'scoring'
  | 'type_pending'
  | 'scored'
  | 'finalized'
  | 'failed';

export type TypeSource = 'specified' | 'classified';

export type WebhookEventName =
  | 'evaluation.classified'
  | 'evaluation.scored'
  | 'evaluation.updated'
  | 'evaluation.finalized'
  | 'evaluation.failed';

export type AuditorKind = 'agent' | 'ordinary';

export interface AgentOption {
  id: string;
  name: string;
  description: string;
  provider: string;
}

export interface Auditor {
  id: string;
  name: string;
  kind: AuditorKind;
  agentId?: string;
  agentName?: string;
  description: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DocType {
  id: string;
  code: string;
  name: string;
  description: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Rule {
  id: string;
  name: string;
  standard: string;
  minScore: number;
  maxScore: number;
  passScore: number;
  weight: number;
  isVeto: boolean;
  positiveExample: string;
  negativeExample: string;
  sort: number;
}

export interface RuleSet {
  id: string;
  typeId: string;
  version?: number;
  status: RuleSetStatus;
  isCurrent: boolean;
  passMode: PassMode;
  overallPassScore: number;
  rules: Rule[];
  basedOnVersion?: number;
  createdAt: string;
  publishedAt?: string;
}

export interface Attachment {
  id: string;
  name: string;
  mime: string;
  role: string;
  sort: number;
  parseFailed: boolean;
  excerpt: string;
}

export interface Evidence {
  fileId: string;
  fileName: string;
  location?: string;
  quote: string;
  locationFound: boolean;
}

export interface Annotation {
  id: string;
  target: 'rule' | 'file';
  ruleId?: string;
  fileId?: string;
  location?: string;
  content: string;
  actor: string;
  createdAt: string;
}

export interface RuleResult {
  ruleId: string;
  ruleName: string;
  standard: string;
  minScore: number;
  maxScore: number;
  passScore: number;
  weight: number;
  isVeto: boolean;
  machineScore?: number;
  machineRationale?: string;
  evidence: Evidence[];
  humanScore?: number;
  humanReason?: string;
  failed: boolean;
  failReason?: string;
}

export interface TimelineEvent {
  id: string;
  at: string;
  actor: string;
  title: string;
  detail?: string;
}

export interface WebhookLog {
  id: string;
  event: WebhookEventName;
  taskId: string;
  bizId: string;
  at: string;
  payload: Record<string, unknown>;
  status: 'success' | 'failed' | 'skipped';
  error?: string;
  retry: number;
}

export interface EvaluationTask {
  id: string;
  bizId: string;
  isTrial: boolean;
  typeId?: string;
  typeCode?: string;
  typeName?: string;
  typeSource?: TypeSource;
  auditorId?: string;
  auditorName?: string;
  auditorKind?: AuditorKind;
  agentName?: string;
  classifyConfidence?: number;
  classifyReason?: string;
  ruleSetId?: string;
  ruleSetVersion?: number;
  passMode?: PassMode;
  overallPassScore?: number;
  status: TaskStatus;
  failReason?: string;
  attachments: Attachment[];
  results: RuleResult[];
  totalScore?: number;
  passed?: boolean;
  complete: boolean;
  annotations: Annotation[];
  timeline: TimelineEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface Credential {
  id: string;
  name: string;
  keyPrefix: string;
  lastSecret?: string;
  enabled: boolean;
  createdAt: string;
}

export interface IntegrationSettings {
  callbackUrl: string;
  subscribedEvents: WebhookEventName[];
  simulateWebhookFail: boolean;
  classifyThreshold: number;
}

export interface AppState {
  types: DocType[];
  ruleSets: RuleSet[];
  auditors: Auditor[];
  agents: AgentOption[];
  tasks: EvaluationTask[];
  credentials: Credential[];
  settings: IntegrationSettings;
  webhookLogs: WebhookLog[];
}

export interface ApiError {
  message: string;
}
