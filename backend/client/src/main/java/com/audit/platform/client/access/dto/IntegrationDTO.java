package com.audit.platform.client.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationDTO {
    private String num;
    private String callbackUrl;
    private String subscribedEvents;
    private BigDecimal classifyThreshold;
}
