package com.audit.platform.client.auditor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditorUpdateParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotBlank
    private String name;
    @NotBlank
    private String kind;
    private String agentNum;
    private String description;
}
