package com.audit.platform.domain.evaluation.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleScoreVO {
    private String ruleNum;
    private BigDecimal score;
    private String rationale;
    private String evidenceJson;
    private Boolean failed;
    private String failReason;
}
