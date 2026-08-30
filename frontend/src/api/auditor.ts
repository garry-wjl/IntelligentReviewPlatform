import { get, post } from './http';
import type { AgentOptionDTO, AuditorDTO, PageDTO } from './dto';
import { asPage } from './dto';

const PREFIX = '/auditor';

export function createAuditor(body: { name: string; kind: string; agentNum?: string; description?: string }) {
  return post<{ num: string }>(`${PREFIX}/command/create`, body);
}

export function updateAuditor(body: {
  num: string;
  name: string;
  kind: string;
  agentNum?: string;
  description?: string;
}) {
  return post<void>(`${PREFIX}/command/update`, body);
}

export function setAuditorEnabled(body: { num: string; enabled: boolean }) {
  return post<void>(`${PREFIX}/command/set-enabled`, body);
}

export function syncAgents() {
  return post<{ count?: number }>(`${PREFIX}/command/sync-agents`, {});
}

export async function pageAuditors(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
  kind?: string;
  enabled?: boolean;
}) {
  return asPage(await get<PageDTO<AuditorDTO>>(`${PREFIX}/query/page`, query));
}

export function getAuditorDetail(num: string) {
  return get<AuditorDTO>(`${PREFIX}/query/detail`, { num });
}

export function listAgents() {
  return get<AgentOptionDTO[] | { list?: AgentOptionDTO[] }>(`${PREFIX}/query/agents`);
}
