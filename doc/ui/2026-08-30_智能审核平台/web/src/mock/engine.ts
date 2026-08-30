import type {
  Attachment,
  EvaluationTask,
  PassMode,
  Rule,
  RuleResult,
  RuleSet,
} from '../types/models';

export const PASS_MODE_LABEL: Record<PassMode, string> = {
  all_pass: '全部通过',
  weighted_sum: '加权总分',
  veto_weighted: '红线 + 加权',
};

export const PASS_MODE_HINT: Record<PassMode, string> = {
  all_pass: '每一条展示分都达到该条通过分，整单才通过。权重与红线标记不参与判定。',
  weighted_sum: 'Σ(展示分 × 权重) 达到总分通过线则整单通过。',
  veto_weighted: '红线条必须通过且不计入加权；其余规则加权后达到总分通过线。',
};

export function displayScore(r: RuleResult): number | undefined {
  if (r.failed) return undefined;
  if (r.humanScore != null) return r.humanScore;
  return r.machineScore;
}

export function rulePassed(r: RuleResult): boolean | undefined {
  const score = displayScore(r);
  if (score == null) return undefined;
  return score >= r.passScore;
}

export function summarize(
  results: RuleResult[],
  passMode: PassMode,
  overallPassScore: number,
): { totalScore?: number; passed: boolean; complete: boolean } {
  const complete = results.length > 0 && results.every((r) => !r.failed);
  const hasAnyScore = results.some((r) => displayScore(r) != null);

  if (passMode === 'all_pass') {
    const passed = complete && results.every((r) => rulePassed(r) === true);
    return { passed, complete };
  }

  if (passMode === 'weighted_sum') {
    const total = results.reduce((sum, r) => {
      const s = displayScore(r);
      return s == null ? sum : sum + s * r.weight;
    }, 0);
    const passed = complete && hasAnyScore && total >= overallPassScore;
    return { totalScore: hasAnyScore ? round2(total) : undefined, passed, complete };
  }

  const vetos = results.filter((r) => r.isVeto);
  const others = results.filter((r) => !r.isVeto);
  const vetoOk = vetos.every((r) => !r.failed && rulePassed(r) === true);
  const othersComplete = others.every((r) => !r.failed);
  const total = others.reduce((sum, r) => {
    const s = displayScore(r);
    return s == null ? sum : sum + s * r.weight;
  }, 0);
  const passed = vetoOk && othersComplete && total >= overallPassScore;
  return { totalScore: others.some((r) => displayScore(r) != null) ? round2(total) : undefined, passed, complete };
}

export function validateRule(rule: Pick<Rule, 'minScore' | 'maxScore' | 'passScore' | 'weight'>, requireWeight: boolean): string | null {
  if (rule.minScore >= rule.maxScore) return '最低分必须小于最高分';
  if (rule.passScore < rule.minScore || rule.passScore > rule.maxScore) return '通过分必须落在最低分与最高分之间';
  if (requireWeight && !(rule.weight > 0)) return '当前评估分方式需要填写大于 0 的权重';
  return null;
}

export function validateRuleSet(rs: RuleSet): string[] {
  const errors: string[] = [];
  if (rs.rules.length === 0) errors.push('至少需要一条规则');
  const needWeight = rs.passMode !== 'all_pass';
  if (needWeight && !(rs.overallPassScore > 0)) errors.push('加权类模式需要配置总分通过线');
  if (rs.passMode === 'veto_weighted' && !rs.rules.some((r) => r.isVeto)) {
    errors.push('红线 + 加权模式至少需要一条红线规则');
  }
  rs.rules.forEach((r, i) => {
    const err = validateRule(r, needWeight);
    if (err) errors.push(`规则「${r.name || `#${i + 1}`}」：${err}`);
  });
  return errors;
}

export function ruleCompleteness(rule: Rule): '完整' | '缺正反例' {
  return rule.positiveExample.trim() && rule.negativeExample.trim() ? '完整' : '缺正反例';
}

export function isUnparseable(name: string): boolean {
  return /损坏|corrupt|\.exe$/i.test(name);
}

export function shouldFailRule(files: Attachment[], index: number, total: number): boolean {
  if (files.some((f) => /超时|timeout/i.test(f.name)) && index === total - 1) return true;
  if (files.some((f) => /全部失败|allfail/i.test(f.name))) return true;
  return false;
}

export function classifyFromFiles(files: Attachment[]): {
  code?: string;
  confidence: number;
  reason: string;
} {
  const names = files.map((f) => f.name).join(' ');
  if (/未知|unknown|杂件/i.test(names)) {
    return { code: 'RS-0001', confidence: 0.41, reason: '文件名含糊，正文特征不足，无法稳定匹配规则集。' };
  }
  if (/季报|季度/i.test(names)) {
    return { code: 'RS-0002', confidence: 0.91, reason: '文件名与章节结构更接近季度工作报告规则集。' };
  }
  if (/尽调|due/i.test(names)) {
    return { code: 'RS-0003', confidence: 0.88, reason: '材料中出现尽调目录与主体信息章节。' };
  }
  return { code: 'RS-0001', confidence: 0.86, reason: '标题与章节更接近立项报告规则集（含目标、风险、资源）。' };
}

export function mockMachineScore(rule: Rule, files: Attachment[]): number {
  const parsed = files.filter((f) => !f.parseFailed);
  let score = rule.minScore + hash(`${rule.id}:${parsed.map((f) => f.name).join('|')}`) % (rule.maxScore - rule.minScore + 1);
  if (parsed.some((f) => /优秀|完整|达标/i.test(f.name))) {
    score = Math.max(score, Math.min(rule.maxScore, rule.passScore + 2));
  }
  if (rule.isVeto && parsed.some((f) => /缺风险|无应对/i.test(f.name))) {
    score = Math.min(score, Math.max(rule.minScore, rule.passScore - 2));
  }
  if (/风险披露/.test(rule.name) && parsed.some((f) => /缺风险|无应对/i.test(f.name))) {
    score = Math.min(score, rule.passScore - 2);
  }
  return clamp(score, rule.minScore, rule.maxScore);
}

export function mockRationale(rule: Rule, score: number, files: Attachment[]): string {
  const fileHint = files.filter((f) => !f.parseFailed).map((f) => f.name).slice(0, 2).join('、') || '内容包';
  if (score >= rule.passScore) {
    return `依据「${fileHint}」中的相关表述，${rule.name}达到通过线。${rule.positiveExample ? `接近正例：${rule.positiveExample.slice(0, 36)}` : ''}`.trim();
  }
  return `依据「${fileHint}」的现有内容，${rule.name}偏弱。${rule.negativeExample ? `更接近反例：${rule.negativeExample.slice(0, 36)}` : '缺少可核验的具体说明。'}`.trim();
}

export function mockEvidence(files: Attachment[]): EvidenceLike[] {
  const ok = files.filter((f) => !f.parseFailed);
  if (ok.length === 0) return [];
  const primary = ok[0];
  const secondary = ok[1];
  const list: EvidenceLike[] = [
    {
      fileId: primary.id,
      fileName: primary.name,
      location: /xlsx|xls|csv/i.test(primary.name) ? 'Sheet1!A1:D20' : '第 12 页',
      quote: primary.excerpt || '……相关段落摘录……',
      locationFound: true,
    },
  ];
  if (secondary) {
    list.push({
      fileId: secondary.id,
      fileName: secondary.name,
      quote: secondary.excerpt || '……附件中的补充说明……',
      locationFound: false,
    });
  }
  return list;
}

export function snapshotResults(rules: Rule[], files: Attachment[], mode: 'agent' | 'ordinary' = 'agent'): RuleResult[] {
  const sorted = [...rules].sort((a, b) => a.sort - b.sort);
  return sorted.map((rule, index) => {
    if (mode === 'agent' && shouldFailRule(files, index, sorted.length)) {
      return {
        ruleId: rule.id,
        ruleName: rule.name,
        standard: rule.standard,
        minScore: rule.minScore,
        maxScore: rule.maxScore,
        passScore: rule.passScore,
        weight: rule.weight,
        isVeto: rule.isVeto,
        evidence: [],
        failed: true,
        failReason: '模拟 Agent 超时，本条未返回分数。',
      };
    }
    const parsed = files.filter((f) => !f.parseFailed);
    const machineScore =
      mode === 'ordinary'
        ? parsed.length
          ? rule.passScore
          : rule.minScore
        : mockMachineScore(rule, files);
    return {
      ruleId: rule.id,
      ruleName: rule.name,
      standard: rule.standard,
      minScore: rule.minScore,
      maxScore: rule.maxScore,
      passScore: rule.passScore,
      weight: rule.weight,
      isVeto: rule.isVeto,
      machineScore,
      machineRationale:
        mode === 'ordinary'
          ? `普通审核器未调用 Agent。${parsed.length ? '材料可解析，按通过分给出基线分。' : '材料无法解析，给出最低分。'}`
          : mockRationale(rule, machineScore, files),
      evidence: mockEvidence(files),
      failed: false,
    };
  });
}

export function applySummary(task: EvaluationTask): EvaluationTask {
  if (!task.passMode) return { ...task, complete: false };
  const summary = summarize(task.results, task.passMode, task.overallPassScore ?? 0);
  return { ...task, ...summary };
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}

function hash(input: string): number {
  let h = 0;
  for (let i = 0; i < input.length; i += 1) h = (h * 31 + input.charCodeAt(i)) >>> 0;
  return h;
}

type EvidenceLike = {
  fileId: string;
  fileName: string;
  location?: string;
  quote: string;
  locationFound: boolean;
};
