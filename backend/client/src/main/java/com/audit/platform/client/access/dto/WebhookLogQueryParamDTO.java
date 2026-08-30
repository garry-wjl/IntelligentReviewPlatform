package com.audit.platform.client.access.dto;

import com.audit.platform.client.common.dto.PageQueryParamDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookLogQueryParamDTO extends PageQueryParamDTO {
    private String num;
    private String name;
    private String evaluationNum;
    private String status;
}
