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
public class RuleItemSnapshotVO {
    private String ruleNum;
    private String name;
    private String standard;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal passScore;
    private BigDecimal weight;
    private Boolean veto;
    private String engineKind;
    private String engineConfigJson;
    private String auditorNum;
    private String auditorKind;
    private String agentName;
}
