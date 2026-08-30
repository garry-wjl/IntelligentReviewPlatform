package com.audit.platform.client.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationBizQueryParamDTO {
    private String operatorId;
    @NotBlank
    private String bizId;
    private String credentialNum;
    private Boolean trial;
}
