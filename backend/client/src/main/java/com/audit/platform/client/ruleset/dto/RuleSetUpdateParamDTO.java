package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleSetUpdateParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String name;
    private String description;
}
