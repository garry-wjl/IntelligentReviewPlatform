import { listAgents, pageAuditors } from '../api/auditor';
import { pageRuleSets } from '../api/ruleset';
import { listEnabledScenes } from '../api/scene';
import { toAgent, toAuditor, toDocType, toScene } from '../api/adapters';
import type { AgentOption, Auditor, DocType, Scene } from '../types/models';

function asAgentList(raw: unknown): AgentOption[] {
  if (Array.isArray(raw)) return raw.map(toAgent);
  if (raw && typeof raw === 'object' && Array.isArray((raw as { list?: unknown[] }).list)) {
    return ((raw as { list: Parameters<typeof toAgent>[0][] }).list ?? []).map(toAgent);
  }
  return [];
}

export async function loadEnabledScenes(): Promise<Scene[]> {
  return (await listEnabledScenes()).map(toScene);
}

export async function loadEnabledRuleSets(): Promise<DocType[]> {
  const page = await pageRuleSets({ pageNo: 1, pageSize: 200, enabled: true });
  return (page.list ?? []).map(toDocType);
}

export async function loadAllRuleSets(): Promise<DocType[]> {
  const page = await pageRuleSets({ pageNo: 1, pageSize: 200 });
  return (page.list ?? []).map(toDocType);
}

export async function loadAuditors(enabledOnly = false): Promise<Auditor[]> {
  const page = await pageAuditors({ pageNo: 1, pageSize: 200, enabled: enabledOnly ? true : undefined });
  return (page.list ?? []).map(toAuditor);
}

export async function loadAgents(): Promise<AgentOption[]> {
  return asAgentList(await listAgents());
}

export function nameMap(items: { id: string; name: string }[]) {
  return Object.fromEntries(items.map((item) => [item.id, item.name]));
}

export function isTaskLevelAuditor(auditorNum?: string | null): auditorNum is string {
  return !!auditorNum && auditorNum !== '-';
}

export function auditorDisplay(task: { auditorId?: string; auditorName?: string }) {
  if (!isTaskLevelAuditor(task.auditorId)) return '按规则指定';
  return task.auditorName ?? task.auditorId ?? '—';
}
