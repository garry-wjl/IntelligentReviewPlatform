package com.audit.platform.client.access.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookEnqueueParamDTO {
    private String operatorId;
    @NotBlank
    private String evaluationNum;
    @NotBlank
    private String eventName;
}
