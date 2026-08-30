package com.audit.platform.infra.common.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.infra.common.client.dto.ScoreResultDTO;
import com.audit.platform.infra.common.client.param.ScoreRequestParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mock 打分：普通审核器返回通过分，Agent 在区间内随机。
 */
@Component
public class ScoringClient {

    public ScoreResultDTO score(ScoreRequestParam param) {
        ScoreResultDTO result = new ScoreResultDTO();
        if ("ORDINARY".equals(param.getAuditorKind())) {
            result.setScore(param.getPassScore());
            result.setRationale("普通审核器按通过分计");
        } else {
            BigDecimal min = param.getMinScore() == null ? BigDecimal.ZERO : param.getMinScore();
            BigDecimal max = param.getMaxScore() == null ? min : param.getMaxScore();
            BigDecimal span = max.subtract(min);
            BigDecimal score = min.add(span.multiply(BigDecimal.valueOf(RandomUtil.randomDouble())));
            result.setScore(score.setScale(4, RoundingMode.HALF_UP));
            result.setRationale(StrUtil.format("Agent mock 打分（{}）", StrUtil.blankToDefault(param.getAgentName(), "agent")));
        }
        result.setEvidenceJson("{\"source\":\"mock\",\"ruleNum\":\"" + StrUtil.blankToDefault(param.getRuleNum(), "") + "\"}");
        return result;
    }
}
