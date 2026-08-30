package com.audit.platform.infra.access.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("webhook_delivery")
public class WebhookDeliveryEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 投递编码 */
    private String num;
    /** 幂等键 */
    private String eventId;
    /** 任务编码 */
    private String evaluationNum;
    /** 业务单号 */
    private String bizId;
    /** 事件名 */
    private String eventName;
    /** 载荷 JSON */
    private String payloadJson;
    /** PENDING/RETRY/SUCCESS/DEAD */
    private String status;
    /** 重试次数 */
    private Integer retryCount;
    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;
    /** 最近错误 */
    private String lastError;
    /** 任务级回调覆盖 */
    private String callbackUrl;
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
