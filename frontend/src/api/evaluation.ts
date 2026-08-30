import { TERMINAL_TASK_STATUSES } from './adapters';
import type { AttachmentParam, EvaluationCreatedDTO, EvaluationDetailDTO, EvaluationListDTO, PageDTO } from './dto';
import { asPage } from './dto';
import { get, post } from './http';

const PREFIX = '/evaluation';

export function createTrial(body: {
  bizId: string;
  auditorNum?: string;
  ruleSetNum?: string;
  ruleSetVersionNum?: string;
  trial?: boolean;
  inputText?: string;
  extraParams?: Record<string, string>;
  attachments: AttachmentParam[];
}) {
  return post<EvaluationCreatedDTO>(`${PREFIX}/command/trial`, { ...body, trial: true });
}

export async function pageEvaluations(query: {
  pageNo?: number;
  pageSize?: number;
  num?: string;
  name?: string;
  keyword?: string;
  bizId?: string;
  ruleSetNum?: string;
  auditorNum?: string;
  status?: string;
  isTrial?: boolean;
  createTimeFrom?: string;
  createTimeTo?: string;
}) {
  return asPage(await get<PageDTO<EvaluationListDTO>>(`${PREFIX}/query/page`, query));
}

export function getEvaluationDetail(num: string) {
  return get<EvaluationDetailDTO>(`${PREFIX}/query/detail`, { num });
}

export function getAttachmentUrl(evaluationNum: string, fileNum: string) {
  return get<{ url: string; fileName?: string }>(`${PREFIX}/query/attachment-url`, { evaluationNum, fileNum });
}

function sleep(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function pollEvaluationUntilSettled(num: string, intervalMs = 1000, maxTimes = 30) {
  let latest = await getEvaluationDetail(num);
  for (let i = 0; i < maxTimes; i += 1) {
    const status = (latest.status ?? '').toUpperCase();
    if (TERMINAL_TASK_STATUSES.has(status)) return latest;
    await sleep(intervalMs);
    latest = await getEvaluationDetail(num);
  }
  return latest;
}
