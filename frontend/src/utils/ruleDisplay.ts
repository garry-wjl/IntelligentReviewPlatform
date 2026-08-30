import type { PassMode, Rule, RuleResult, RuleSet } from '../types/models';

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

export function ruleCompleteness(rule: Rule): '完整' | '缺正反例' {
  return rule.positiveExample.trim() && rule.negativeExample.trim() ? '完整' : '缺正反例';
}

export function validateRule(
  rule: Pick<Rule, 'minScore' | 'maxScore' | 'passScore' | 'weight'>,
  requireWeight: boolean,
): string | null {
  if (rule.minScore >= rule.maxScore) return '最低分必须小于最高分';
  if (rule.passScore < rule.minScore || rule.passScore > rule.maxScore) return '通过分必须落在最低分与最高分之间';
  if (requireWeight && !(rule.weight > 0)) return '当前评估分方式需要填写大于 0 的权重';
  return null;
}

export function validateRuleSet(rs: RuleSet): string[] {
  const errors: string[] = [];
  if (rs.rules.length === 0) errors.push('至少需要一条规则');
  rs.rules.forEach((r, i) => {
    if (!r.auditorId) errors.push(`规则「${r.name || `#${i + 1}`}」：必须选择审核器`);
  });
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
