package com.audit.platform.infra.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation_attachment")
public class EvaluationAttachmentEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 附件编码 */
    private String num;
    /** 任务编码 */
    private String evaluationNum;
    /** TOS 对象键 */
    private String objectKey;
    /** 文件名 */
    private String fileName;
    /** MIME */
    private String mime;
    /** 角色 main/appendix */
    private String role;
    /** 排序 */
    private Integer sortNo;
    /** 解析是否失败 */
    private Boolean parseFailed;
    /** 解析摘录 */
    private String excerpt;
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
