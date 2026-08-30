package com.audit.platform.domain.ruleset.valueobject;

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
public class RuleItemVO {
    private Long id;
    private String num;
    private String name;
    private String standard;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal passScore;
    private BigDecimal weight;
    private Boolean veto;
    private String positiveExample;
    private String negativeExample;
    private Integer sortNo;
    /** ORDINARY / AGENT */
    private String engineKind;
    /** ordinary: {checks:[{paramKey,op,value}]}；agent: {paramKeys:[...]} */
    private String engineConfigJson;
    /** 本条规则使用的审核器 */
    private String auditorNum;
    /** 审核器名称快照 */
    private String auditorName;
}
