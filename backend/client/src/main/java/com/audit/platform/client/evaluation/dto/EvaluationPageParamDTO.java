package com.audit.platform.client.evaluation.dto;

import com.audit.platform.client.common.dto.PageQueryParamDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationPageParamDTO extends PageQueryParamDTO {
    private String num;
    private String name;
    private String bizId;
    private String ruleSetNum;
    private String auditorNum;
    private String status;
    private Boolean isTrial;
    private String createTimeFrom;
    private String createTimeTo;
}
