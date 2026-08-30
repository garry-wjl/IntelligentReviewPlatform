package com.audit.platform.domain.evaluation.valueobject;

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
public class ClassifyResultVO {
    private String ruleSetNum;
    private BigDecimal confidence;
    private String reason;
}
