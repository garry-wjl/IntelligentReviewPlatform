package com.audit.platform.client.evaluation.dto;

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
public class EvaluationDetailDTO {
    private String num;
    private String bizId;
    private Boolean trial;
    private String status;
    private String auditorNum;
    private String auditorKind;
    private String agentName;
    private String ruleSetNum;
    private String ruleSetVersionNum;
    private Integer ruleSetVersionNo;
    private String ruleSetSource;
    private BigDecimal classifyConfidence;
    private String classifyReason;
    private String scoreMode;
    private BigDecimal overallPassScore;
    private BigDecimal totalScore;
    private Boolean passed;
    private Boolean complete;
    private String failReason;
    private String callbackUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Builder.Default
    private List<AttachmentDTO> attachments = new ArrayList<>();
    @Builder.Default
    private List<RuleResultDTO> results = new ArrayList<>();
    @Builder.Default
    private List<AnnotationDTO> annotations = new ArrayList<>();
    @Builder.Default
    private List<TimelineDTO> timeline = new ArrayList<>();
}
