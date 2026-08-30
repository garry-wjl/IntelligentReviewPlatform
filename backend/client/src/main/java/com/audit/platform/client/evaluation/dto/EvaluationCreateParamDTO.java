package com.audit.platform.client.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EvaluationCreateParamDTO {
    private String operatorId;
    private String credentialNum;
    @NotBlank
    private String bizId;
    private String auditorNum;
    private String ruleSetNum;
    private String ruleSetVersionNum;
    private Boolean trial;
    private String callbackUrl;
    private String inputText;
    private Map<String, String> extraParams;
    @Valid
    @NotEmpty
    private List<AttachmentParamDTO> attachments;
}
