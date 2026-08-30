package com.audit.platform.client.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SceneNumParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
}
