package com.audit.platform.client.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EvaluationScoreParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String ruleNum;
    @NotNull
    private BigDecimal score;
    @NotBlank
    private String reason;
}
