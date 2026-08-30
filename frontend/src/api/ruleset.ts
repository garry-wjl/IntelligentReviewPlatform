import { get, post } from './http';
import type { PageDTO, RuleSetDTO, RuleSetDetailDTO, RuleSetVersionDTO } from './dto';
import { asPage } from './dto';

const PREFIX = '/ruleset';

export function createRuleSet(body: { name: string; description?: string; sceneNum: string }) {
  return post<{ num: string }>(`${PREFIX}/command/create`, body);
}

export function updateRuleSet(body: { num: string; name: string; description?: string }) {
  return post<void>(`${PREFIX}/command/update`, body);
}

export function setRuleSetEnabled(body: { num: string; enabled: boolean }) {
  return post<void>(`${PREFIX}/command/set-enabled`, body);
}

export function createDraft(body: { num: string; basedOnVersionNum?: string }) {
  return post<{ versionNum: string; versionNo?: number }>(`${PREFIX}/command/create-draft`, body);
}

export function changeScoreMode(body: {
  num: string;
  scoreMode: string;
  overallPassScore?: number;
}) {
  return post<void>(`${PREFIX}/command/score-mode`, body);
}

export function upsertRule(body: {
  num: string;
  versionNum: string;
  ruleNum?: string;
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
  auditorNum: string;
  checks?: { paramKey: string; op: string; value?: string }[];
  agentParamKeys?: string[];
}) {
  return post<{ ruleNum: string }>(`${PREFIX}/command/upsert-rule`, body);
}

export function removeRule(body: { num: string; versionNum: string; ruleNum: string }) {
  return post<void>(`${PREFIX}/command/remove-rule`, body);
}

export function moveRule(body: { num: string; versionNum: string; ruleNum: string; direction: number }) {
  return post<void>(`${PREFIX}/command/move-rule`, body);
}

export function publishRuleSet(body: { num: string }) {
  return post<{ versionNo?: number; versionNum?: string }>(`${PREFIX}/command/publish`, body);
}

export function disableCurrent(body: { num: string }) {
  return post<void>(`${PREFIX}/command/disable-current`, body);
}

export async function pageRuleSets(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
  enabled?: boolean;
}) {
  return asPage(await get<PageDTO<RuleSetDTO>>(`${PREFIX}/query/page`, query));
}

export function getRuleSetDetail(num: string) {
  return get<RuleSetDetailDTO>(`${PREFIX}/query/detail`, { num });
}

export function getRuleSetVersion(versionNum: string) {
  return get<RuleSetVersionDTO>(`${PREFIX}/query/version`, { versionNum });
}
