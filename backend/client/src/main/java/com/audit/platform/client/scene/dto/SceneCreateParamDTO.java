package com.audit.platform.client.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SceneCreateParamDTO {
    private String operatorId;
    @NotBlank
    private String name;
    private String description;
    private List<SceneExtraParamDTO> extraParams = new ArrayList<>();
}
