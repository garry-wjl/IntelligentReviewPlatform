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
public class AuditorDTO {
    private String num;
    private String name;
    private String kind;
    private String agentNum;
    private String agentName;
    private String description;
    private Boolean enabled;
}
