package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleMoveParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String versionNum;
    @NotBlank
    private String ruleNum;
    @NotNull
    private Integer direction;
}
