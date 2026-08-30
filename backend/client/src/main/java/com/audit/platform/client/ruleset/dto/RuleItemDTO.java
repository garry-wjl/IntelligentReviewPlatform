package com.audit.platform.client.ruleset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleItemDTO {
    private String num;
    private String name;
    private String standard;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal passScore;
    private BigDecimal weight;
    private Boolean veto;
    private String positiveExample;
    private String negativeExample;
    private Integer sortNo;
    private String engineKind;
    private List<RuleCheckDTO> checks;
    private List<String> agentParamKeys;
    private String auditorNum;
    private String auditorName;
}
