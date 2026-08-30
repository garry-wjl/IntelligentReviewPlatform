package com.audit.platform.client.auditor.dto;

import com.audit.platform.client.common.dto.PageQueryParamDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditorPageParamDTO extends PageQueryParamDTO {
    private String num;
    private String name;
    private String kind;
    private Boolean enabled;
}
