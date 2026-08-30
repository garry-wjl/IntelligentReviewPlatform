package com.audit.platform.infra.common.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreResultDTO {
    /** 机评分 */
    private BigDecimal score;
    /** 评分理由 */
    private String rationale;
    /** 证据 JSON */
    private String evidenceJson;
}
