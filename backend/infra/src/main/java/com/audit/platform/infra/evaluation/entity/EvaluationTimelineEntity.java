package com.audit.platform.infra.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation_timeline")
public class EvaluationTimelineEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 时间线编码 */
    private String num;
    /** 任务编码 */
    private String evaluationNum;
    /** 操作人 */
    private String actor;
    /** 标题 */
    private String title;
    /** 详情 */
    private String detail;
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
