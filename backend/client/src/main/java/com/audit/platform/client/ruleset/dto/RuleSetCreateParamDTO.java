package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleSetCreateParamDTO {
    private String operatorId;
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String sceneNum;
}
