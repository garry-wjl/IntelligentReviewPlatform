package com.audit.platform.client.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationListDTO {
    private String num;
    private String bizId;
    private Boolean trial;
    private String status;
    private String auditorNum;
    private String auditorKind;
    private String ruleSetNum;
    private Integer ruleSetVersionNo;
    private BigDecimal totalScore;
    private Boolean passed;
    private Boolean complete;
    private LocalDateTime createTime;
}
