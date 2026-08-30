package com.audit.platform.client.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageQueryParamDTO {
    private String operatorId;
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String keyword;
}
