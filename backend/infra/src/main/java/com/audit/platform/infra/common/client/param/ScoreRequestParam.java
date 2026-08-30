package com.audit.platform.infra.common.client.param;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreRequestParam {
    /** 规则编码 */
    private String ruleNum;
    /** 审核器类型 AGENT/ORDINARY */
    private String auditorKind;
    /** Agent 名称 */
    private String agentName;
    /** 最低分 */
    private BigDecimal minScore;
    /** 最高分 */
    private BigDecimal maxScore;
    /** 通过分 */
    private BigDecimal passScore;
    /** 评审标准 */
    private String standard;
    /** 附件摘录 */
    private String excerpt;
}
