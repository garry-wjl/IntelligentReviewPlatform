package com.audit.platform.client.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationNumParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
}
