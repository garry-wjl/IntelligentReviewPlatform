package com.audit.platform.client.access.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class IntegrationUpdateParamDTO {
    private String operatorId;
    private String callbackUrl;
    private String subscribedEvents;
    private BigDecimal classifyThreshold;
}
