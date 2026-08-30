package com.audit.platform.infra.auditor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_catalog")
public class AgentCatalogEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 本地缓存编码 AGC-1 */
    private String num;
    /** 平台 Agent 编码 */
    private String agentNum;
    /** 名称 */
    private String name;
    /** 说明 */
    private String description;
    /** 提供方 */
    private String provider;
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
