package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RuleUpsertParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String versionNum;
    private String ruleNum;
    @NotBlank
    private String name;
    @NotBlank
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
    @NotBlank
    private String auditorNum;
    private List<RuleCheckDTO> checks = new ArrayList<>();
    private List<String> agentParamKeys = new ArrayList<>();
}
