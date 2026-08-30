package com.audit.platform.client.scene.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneDTO {
    private String num;
    private String name;
    private String description;
    private Boolean enabled;
    @Builder.Default
    private List<SceneParamDTO> extraParams = new ArrayList<>();
    @Builder.Default
    private List<SceneParamDTO> params = new ArrayList<>();
    private LocalDateTime updateTime;
}
