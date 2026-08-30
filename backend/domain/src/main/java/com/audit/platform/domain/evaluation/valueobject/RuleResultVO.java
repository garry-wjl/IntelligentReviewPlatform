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
public class RuleResultVO {
    private Long id;
    private String num;
    private String ruleNum;
    private String ruleName;
    private String standard;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal passScore;
    private BigDecimal weight;
    private Boolean veto;
    private BigDecimal machineScore;
    private String machineRationale;
    private BigDecimal humanScore;
    private String humanReason;
    private Boolean failed;
    private String failReason;
    private String evidenceJson;
}
