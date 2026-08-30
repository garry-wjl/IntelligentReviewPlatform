package com.audit.platform.infra.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation_annotation")
public class EvaluationAnnotationEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 标注编码 */
    private String num;
    /** 任务编码 */
    private String evaluationNum;
    /** 目标 RULE/FILE */
    private String target;
    /** 规则编码 */
    private String ruleNum;
    /** 附件编码 */
    private String fileNum;
    /** 位置 */
    private String location;
    /** 标注内容 */
    private String content;
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
