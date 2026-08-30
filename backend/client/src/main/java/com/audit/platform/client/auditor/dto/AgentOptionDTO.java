package com.audit.platform.client.auditor.dto;

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
public class AgentOptionDTO {
    private String agentNum;
    private String name;
    private String description;
    private String provider;
}
