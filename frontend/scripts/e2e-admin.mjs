/**
 * 前后端联调：用管理端 SPA 同一套路径打真实后端。
 * 需要后端已启动（默认 http://localhost:8080）。
 */
const BASE = (process.env.API_BASE || 'http://localhost:8080').replace(/\/$/, '');
const ADMIN = `${BASE}/admin/v1`;
const OPEN = `${BASE}/open/v1`;

function fail(message) {
  console.error(`FAIL ${message}`);
  process.exit(1);
}

async function request(method, url, { body, apiKey } = {}) {
  const headers = { Accept: 'application/json' };
  if (apiKey) {
    headers.Authorization = `Bearer ${apiKey}`;
  } else {
    headers.Authorization = 'Bearer dev-sso';
    headers['X-Operator-Id'] = 'e2e-tester';
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let payload;
  try {
    payload = JSON.parse(text);
  } catch {
    fail(`${method} ${url} 非 JSON: HTTP ${response.status} ${text.slice(0, 200)}`);
  }
  if (payload.code !== 200) {
    fail(`${method} ${url} code=${payload.code} msg=${payload.msg || payload.message} body=${text}`);
  }
  return payload.data;
}

async function get(path, query) {
  const url = new URL(path.startsWith('http') ? path : `${ADMIN}${path}`);
  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, String(value));
    });
  }
  return request('GET', url.toString());
}

async function post(path, body) {
  return request('POST', `${ADMIN}${path}`, { body });
}

async function postOpen(path, apiKey, body) {
  return request('POST', `${OPEN}${path}`, { body, apiKey });
}

async function getOpen(path, apiKey, query) {
  const url = new URL(`${OPEN}${path}`);
  if (query) {
    Object.entries(query).forEach(([key, value]) => url.searchParams.set(key, String(value)));
  }
  return request('GET', url.toString(), { apiKey });
}

async function waitStatus(num, expected, maxTimes = 40) {
  for (let i = 0; i < maxTimes; i += 1) {
    const detail = await get('/evaluation/query/detail', { num });
    const status = detail.status;
    if (status === expected || status === 'FAILED' || status === 'TYPE_PENDING') {
      if (status !== expected) fail(`任务 ${num} 期望 ${expected} 实际 ${status} ${JSON.stringify(detail)}`);
      return detail;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  fail(`任务 ${num} 未在超时内到达 ${expected}`);
}

async function main() {
  const health = await fetch(`${BASE}/admin/v1/access/query/integration`, {
    headers: { Authorization: 'Bearer dev-sso' },
  }).catch((e) => {
    fail(`无法连接后端 ${BASE}: ${e.message}`);
  });
  if (!health.ok && health.status >= 500) fail(`后端不可用 HTTP ${health.status}`);

  const scene = await post('/scene/command/create', {
    name: '联调场景-FE',
    description: '前端联调',
    extraParams: [{ key: 'projectName', label: '项目名称' }],
  });
  if (!String(scene.num).startsWith('SCN-')) fail(`场景编号异常 ${scene.num}`);
  const scenePage = await get('/scene/query/page', { pageNo: 1, pageSize: 20, name: '联调场景-FE' });
  if ((scenePage.total ?? 0) < 1) fail('场景分页未命中');

  const created = await post('/ruleset/command/create', {
    name: '联调规则集-FE',
    description: '前端联调',
    sceneNum: scene.num,
  });
  if (!String(created.num).startsWith('RS-')) fail(`规则集编号异常 ${created.num}`);

  const detail = await get('/ruleset/query/detail', { num: created.num });
  const versionNum = detail.versions?.[0]?.num;
  if (!versionNum) fail('规则集无草稿版本');

  const auditor = await post('/auditor/command/create', {
    name: '联调普通审核器',
    kind: 'ORDINARY',
    description: '基线分',
  });
  const auditorPage = await get('/auditor/query/page', { pageNo: 1, pageSize: 20, keyword: '联调普通' });
  if ((auditorPage.total ?? 0) < 1) fail('审核器分页未命中');
  const agents = await get('/auditor/query/agents');
  if (!Array.isArray(agents) || agents.length < 1) fail('Agent 目录为空');

  await post('/ruleset/command/score-mode', {
    num: created.num,
    scoreMode: 'ALL_PASS',
    overallPassScore: 0,
  });
  const rule = await post('/ruleset/command/upsert-rule', {
    num: created.num,
    versionNum,
    name: '结构完整',
    standard: '章节齐全、论证充分',
    minScore: 0,
    maxScore: 10,
    passScore: 6,
    weight: 1,
    veto: false,
    engineKind: 'ORDINARY',
    auditorNum: auditor.num,
    checks: [{ paramKey: 'Input', op: 'NOT_BLANK' }],
  });
  if (!String(rule.ruleNum).startsWith('RUL-')) fail(`规则编号异常 ${rule.ruleNum}`);
  const published = await post('/ruleset/command/publish', { num: created.num });
  if (published.versionNo !== 1) fail(`发布版本号异常 ${published.versionNo}`);

  const page = await get('/ruleset/query/page', { pageNo: 1, pageSize: 20, keyword: '联调规则集-FE' });
  if ((page.total ?? 0) < 1) fail('规则集分页未命中刚创建的记录');

  const trial = await post('/evaluation/command/trial', {
    bizId: `TRIAL-FE-${Date.now()}`,
    ruleSetNum: created.num,
    ruleSetVersionNum: versionNum,
    trial: true,
    inputText: '章节齐全、论证充分的立项报告正文',
    attachments: [{ fileName: 'report.pdf', mime: 'application/pdf', role: 'main' }],
  });
  const scored = await waitStatus(trial.num, 'SCORED');
  if (scored.passed !== true || scored.complete !== true) fail(`试评结果异常 ${JSON.stringify(scored)}`);
  if (scored.auditorNum && scored.auditorNum !== '-') fail(`未指定任务审核器时期望 auditorNum=-，实际 ${scored.auditorNum}`);
  if (Number(scored.results?.[0]?.machineScore) !== 6) fail(`普通审核器机评分应为 6，实际 ${scored.results?.[0]?.machineScore}`);

  const tasks = await get('/evaluation/query/page', { pageNo: 1, pageSize: 10, isTrial: true });
  if ((tasks.total ?? 0) < 1) fail('任务分页未命中试评');

  await get('/access/query/integration');
  const secret = await post('/access/command/create-credential', { name: '联调密钥-FE' });
  if (!secret.rawSecret) fail('未返回 rawSecret');

  const openCreated = await postOpen('/evaluation/command/create', secret.rawSecret, {
    bizId: `APP-FE-${Date.now()}`,
    ruleSetNum: created.num,
    inputText: '开放接口提交的正文',
    attachments: [{ fileName: 'app.pdf', fileUrl: 'https://example.com/app.pdf', mime: 'application/pdf' }],
  });
  let openDetail;
  for (let i = 0; i < 40; i += 1) {
    openDetail = await getOpen('/evaluation/query/detail', secret.rawSecret, { num: openCreated.num });
    if (['SCORED', 'FAILED', 'TYPE_PENDING'].includes(openDetail.status)) break;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  if (openDetail?.status !== 'SCORED') fail(`开放任务未打分完成 ${JSON.stringify(openDetail)}`);

  console.log('PASS 前后端联调：场景 / 规则集 / 审核器 / 试评任务 / 开放 API 均已通过');
}

main().catch((e) => fail(e.stack || e.message));
