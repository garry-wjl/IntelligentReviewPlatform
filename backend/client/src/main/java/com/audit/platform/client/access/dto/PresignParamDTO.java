package com.audit.platform.client.access.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresignParamDTO {
    private String operatorId;
    @NotBlank
    private String fileName;
    private String contentType;
}
