package com.audit.platform.client.scene.dto;

import com.audit.platform.client.common.dto.PageQueryParamDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenePageParamDTO extends PageQueryParamDTO {
    private String num;
    private String name;
    private Boolean enabled;
}
