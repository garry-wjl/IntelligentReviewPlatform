package com.audit.platform.domain.auditor.valueobject;

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
public class AgentOptionVO {
    private String agentNum;
    private String name;
    private String description;
    private String provider;
}
