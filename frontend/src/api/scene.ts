import { get, post } from './http';
import type { PageDTO, SceneDTO } from './dto';
import { asPage } from './dto';

const PREFIX = '/scene';

export function createScene(body: {
  name: string;
  description?: string;
  extraParams?: { key: string; label: string }[];
}) {
  return post<{ num: string }>(`${PREFIX}/command/create`, body);
}

export function updateScene(body: {
  num: string;
  name: string;
  description?: string;
  extraParams?: { key: string; label: string }[];
}) {
  return post<void>(`${PREFIX}/command/update`, body);
}

export function setSceneEnabled(body: { num: string; enabled: boolean }) {
  return post<void>(`${PREFIX}/command/set-enabled`, body);
}

export async function pageScenes(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
  enabled?: boolean;
}) {
  return asPage(await get<PageDTO<SceneDTO>>(`${PREFIX}/query/page`, query));
}

export function getSceneDetail(num: string) {
  return get<SceneDTO>(`${PREFIX}/query/detail`, { num });
}

export async function listEnabledScenes() {
  const raw = await get<SceneDTO[] | { list?: SceneDTO[] }>(`${PREFIX}/query/list`);
  if (Array.isArray(raw)) return raw;
  return raw?.list ?? [];
}
