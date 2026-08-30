package com.audit.platform.infra.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_rule_result")
public class EvaluationRuleResultEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 结果编码 */
    private String num;
    /** 任务编码 */
    private String evaluationNum;
    /** 规则编码 */
    private String ruleNum;
    /** 规则名称快照 */
    private String ruleName;
    /** 评审标准快照 */
    private String standard;
    /** 最低分 */
    private BigDecimal minScore;
    /** 最高分 */
    private BigDecimal maxScore;
    /** 通过分 */
    private BigDecimal passScore;
    /** 权重 */
    private BigDecimal weight;
    /** 是否红线 */
    private Boolean veto;
    /** 机评分 */
    private BigDecimal machineScore;
    /** 机评理由 */
    private String machineRationale;
    /** 人工分 */
    private BigDecimal humanScore;
    /** 改分原因 */
    private String humanReason;
    /** 是否打分失败 */
    private Boolean failed;
    /** 失败原因 */
    private String failReason;
    /** 证据 JSON */
    private String evidenceJson;
    /** 创建人 */
    @TableField("create_no")
    private String createId;
    /** 更新人 */
    @TableField("update_no")
    private String updateId;
    /** 软删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
