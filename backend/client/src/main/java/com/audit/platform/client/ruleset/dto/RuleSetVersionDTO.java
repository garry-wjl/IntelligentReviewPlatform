package com.audit.platform.client.ruleset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleSetVersionDTO {
    private String num;
    private String ruleSetNum;
    private Integer versionNo;
    private String status;
    private Boolean currentFlag;
    private String scoreMode;
    private BigDecimal overallPassScore;
    private Integer basedOnVersionNo;
    private LocalDateTime createTime;
    @Builder.Default
    private List<RuleItemDTO> rules = new ArrayList<>();
}
