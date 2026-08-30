package com.audit.platform.domain.evaluation.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleSnapshotVO {
    private String ruleSetNum;
    private String ruleSetVersionNum;
    private Integer versionNo;
    private String scoreMode;
    private BigDecimal overallPassScore;
    @Builder.Default
    private List<RuleItemSnapshotVO> rules = new ArrayList<>();
}
