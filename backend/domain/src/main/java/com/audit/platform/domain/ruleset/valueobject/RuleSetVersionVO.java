package com.audit.platform.domain.ruleset.valueobject;

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
public class RuleSetVersionVO {
    private Long id;
    private String num;
    private Integer versionNo;
    private String status;
    private Boolean currentFlag;
    private String scoreMode;
    private BigDecimal overallPassScore;
    private Integer basedOnVersionNo;
    @Builder.Default
    private List<RuleItemVO> rules = new ArrayList<>();
}
