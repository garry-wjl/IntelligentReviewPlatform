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
@TableName("evaluation")
public class EvaluationEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务编码 EVL-/TRL- */
    private String num;
    /** 调用方业务单号 */
    private String bizId;
    /** 是否试评 */
    @TableField("is_trial")
    private Boolean trial;
    /** 任务状态 */
    private String status;
    /** 审核器编码 */
    private String auditorNum;
    /** 审核器类型快照 */
    private String auditorKind;
    /** Agent 名称快照 */
    private String agentName;
    /** 规则集编码 */
    private String ruleSetNum;
    /** 规则集版本编码 */
    private String ruleSetVersionNum;
    /** 规则集版本号 */
    private Integer ruleSetVersionNo;
    /** SPECIFIED/CLASSIFIED */
    private String ruleSetSource;
    /** 识别置信度 */
    private BigDecimal classifyConfidence;
    /** 识别原因 */
    private String classifyReason;
    /** 评估分方式 */
    private String scoreMode;
    /** 总分通过线 */
    private BigDecimal overallPassScore;
    /** 加权总分 */
    private BigDecimal totalScore;
    /** 是否通过 */
    private Boolean passed;
    /** 是否打分完整 */
    private Boolean complete;
    /** 失败原因 */
    private String failReason;
    /** 开放调用凭证 */
    private String credentialNum;
    /** 任务级回调覆盖 */
    private String callbackUrl;
    /** 场景内置 Input 文本 */
    private String inputText;
    /** 场景扩展参数 JSON */
    private String extraParamsJson;
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
