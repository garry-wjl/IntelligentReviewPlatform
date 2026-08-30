package com.audit.platform.client.auditor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditorNumParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
}
