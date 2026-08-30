package com.audit.platform.client.ruleset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RuleSetScoreModeParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String scoreMode;
    private BigDecimal overallPassScore;
}
