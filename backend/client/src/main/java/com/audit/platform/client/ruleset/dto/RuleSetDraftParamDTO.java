package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleSetDraftParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    private String basedOnVersionNum;
}
