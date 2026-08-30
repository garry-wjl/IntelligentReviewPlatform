package com.audit.platform.infra.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scene")
public class SceneEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务编码 SCN-0001 */
    private String num;
    /** 名称 */
    private String name;
    /** 说明 */
    private String description;
    /** 用户扩展参数 JSON */
    private String extraParamsJson;
    /** 是否启用 */
    private Boolean enabled;
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
