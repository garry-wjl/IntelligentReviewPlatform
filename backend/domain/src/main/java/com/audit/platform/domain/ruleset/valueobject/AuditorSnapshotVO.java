package com.audit.platform.domain.ruleset.valueobject;

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
public class AuditorSnapshotVO {
    private String num;
    private String name;
    private String kind;
    private String agentName;
    private Boolean enabled;
}
