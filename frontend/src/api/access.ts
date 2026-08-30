import { get, post } from './http';
import type { CredentialDTO, CredentialSecretDTO, IntegrationDTO, PageDTO, WebhookLogDTO } from './dto';
import { asPage } from './dto';

const PREFIX = '/access';

export function createCredential(body: { name: string }) {
  return post<CredentialSecretDTO>(`${PREFIX}/command/create-credential`, body);
}

export function disableCredential(body: { num: string }) {
  return post<void>(`${PREFIX}/command/disable-credential`, body);
}

export function updateIntegration(body: {
  callbackUrl?: string;
  subscribedEvents?: string;
  classifyThreshold?: number;
}) {
  return post<void>(`${PREFIX}/command/update-integration`, body);
}

export function replayWebhook(body: { num: string }) {
  return post<void>(`${PREFIX}/command/replay-webhook`, body);
}

export async function pageCredentials(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
}) {
  return asPage(await get<PageDTO<CredentialDTO>>(`${PREFIX}/query/credentials`, query));
}

export function getIntegration() {
  return get<IntegrationDTO>(`${PREFIX}/query/integration`);
}

export async function pageWebhooks(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
  evaluationNum?: string;
  status?: string;
}) {
  return asPage(await get<PageDTO<WebhookLogDTO>>(`${PREFIX}/query/webhooks`, query));
}
