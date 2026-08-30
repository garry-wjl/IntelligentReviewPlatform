package com.audit.platform.client.scene.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SceneEnabledParamDTO {
    private String operatorId;
    @NotBlank
    private String num;
    @NotNull
    private Boolean enabled;
}
