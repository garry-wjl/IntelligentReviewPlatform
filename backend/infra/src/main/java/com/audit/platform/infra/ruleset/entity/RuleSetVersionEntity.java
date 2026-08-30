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
@TableName("rule_set_version")
public class RuleSetVersionEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 版本业务编码 */
    private String num;
    /** 规则集编码 */
    private String ruleSetNum;
    /** 发布版本号 */
    private Integer versionNo;
    /** DRAFT/PUBLISHED/ARCHIVED */
    private String status;
    /** 是否当前发布 */
    private Boolean currentFlag;
    /** 评估分方式 */
    private String scoreMode;
    /** 总分通过线 */
    private BigDecimal overallPassScore;
    /** 基于版本 */
    private Integer basedOnVersionNo;
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
