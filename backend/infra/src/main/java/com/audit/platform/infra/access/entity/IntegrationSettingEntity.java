package com.audit.platform.infra.access.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("integration_setting")
public class IntegrationSettingEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 固定编号 INT-DEFAULT */
    private String num;
    /** 默认回调地址 */
    private String callbackUrl;
    /** 订阅事件 CSV */
    private String subscribedEvents;
    /** 识别阈值 */
    private BigDecimal classifyThreshold;
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
