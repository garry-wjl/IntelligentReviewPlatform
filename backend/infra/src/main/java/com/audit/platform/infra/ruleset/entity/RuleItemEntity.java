package com.audit.platform.infra.ruleset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rule_item")
public class RuleItemEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 规则编码 */
    private String num;
    /** 所属版本编码 */
    private String versionNum;
    /** 规则名称 */
    private String name;
    /** 评审标准 */
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
    /** 正例 */
    private String positiveExample;
    /** 反例 */
    private String negativeExample;
    /** 排序 */
    private Integer sortNo;
    /** 规则器类型 ORDINARY/AGENT */
    private String engineKind;
    /** 规则器配置 JSON */
    private String engineConfigJson;
    /** 本条规则使用的审核器编码 */
    private String auditorNum;
    /** 审核器名称快照 */
    private String auditorName;
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
