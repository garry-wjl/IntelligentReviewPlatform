package com.audit.platform.client.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationAnnotateParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String target;
    private String ruleNum;
    private String fileNum;
    private String location;
    @NotBlank
    private String content;
}
