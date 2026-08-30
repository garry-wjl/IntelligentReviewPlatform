package com.audit.platform.infra.ruleset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_set")
public class RuleSetEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务编码 RS-0001 */
    private String num;
    /** 名称 */
    private String name;
    /** 说明 */
    private String description;
    /** 绑定场景编号 */
    private String sceneNum;
    /** 场景名称快照 */
    private String sceneName;
    /** 创建时场景参数快照 JSON */
    private String sceneParamsJson;
    /** 是否启用 */
    private Boolean enabled;
    /** 评估分方式 */
    private String scoreMode;
    /** 总分通过线 */
    private java.math.BigDecimal overallPassScore;
    /** 当前发布版本编码 */
    private String currentPublishedVersionNum;
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
