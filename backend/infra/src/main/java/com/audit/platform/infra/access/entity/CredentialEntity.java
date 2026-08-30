package com.audit.platform.infra.access.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credential")
public class CredentialEntity {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 凭证编码 */
    private String num;
    /** 名称 */
    private String name;
    /** 密钥前缀 */
    private String keyPrefix;
    /** 密钥 SHA-256 */
    private String secretHash;
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
