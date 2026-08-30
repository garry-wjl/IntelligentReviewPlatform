package com.audit.platform.facade.domain;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class DomainEntity {
    protected Long id;
    protected LocalDateTime createTime;
    protected String createId;
    protected LocalDateTime updateTime;
    protected String updateId;

    public void initialize(String operatorId) {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = this.createTime == null ? now : this.createTime;
        this.createId = this.createId == null ? operatorId : this.createId;
        this.updateTime = now;
        this.updateId = operatorId;
    }

    public void validate() {
        Assert.notNull(this.createId, "实体创建人不能为空");
        Assert.notNull(this.updateId, "实体更新人不能为空");
        Assert.notNull(this.createTime, "实体创建时间不能为空");
        Assert.notNull(this.updateTime, "实体更新时间不能为空");
        domainValidate();
    }

    public abstract void domainValidate();

    public abstract void save(String operatorId);

    public abstract void delete(String operatorId);
}
