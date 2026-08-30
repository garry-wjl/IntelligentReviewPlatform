package com.audit.platform.client.scene.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneParamDTO {
    private String key;
    private String label;
    private String type;
    private Boolean builtin;
}
