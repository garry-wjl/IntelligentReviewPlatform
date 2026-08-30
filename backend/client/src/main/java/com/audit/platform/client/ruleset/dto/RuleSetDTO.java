package com.audit.platform.client.ruleset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleSetDTO {
    private String num;
    private String name;
    private String description;
    private String sceneNum;
    private String sceneName;
    private Boolean enabled;
    private String currentPublishedVersionNum;
    private Integer currentVersionNo;
    private Integer ruleCount;
    private String scoreMode;
    private java.math.BigDecimal overallPassScore;
    private LocalDateTime updateTime;
}
