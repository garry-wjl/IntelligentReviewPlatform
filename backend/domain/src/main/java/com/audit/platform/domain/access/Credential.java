package com.audit.platform.domain.access;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.gateway.AccessGateway;
import com.audit.platform.domain.access.repository.CredentialRepository;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import com.audit.platform.facade.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

/**
 * 开放 API 凭证聚合根。明文密钥只在创建当次由应用层回传，不落库。
 */
@Getter
@Setter
public class Credential extends DomainEntity {
    private String num;
    private String name;
    private String keyPrefix;
    private String secretHash;
    private Boolean enabled;
    /** 仅创建当次内存持有，不持久化。 */
    private String rawSecret;

    private CredentialRepository credentialRepository;
    private AccessGateway accessGateway;
    private DomainEventPublisher domainEventPublisher;

    public Credential() {
    }

    public Credential(String name, CredentialRepository credentialRepository,
                      AccessGateway accessGateway, DomainEventPublisher domainEventPublisher) {
        this.name = name;
        this.credentialRepository = credentialRepository;
        this.accessGateway = accessGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 保存凭证。新建时生成密钥并哈希落库。
     *
     * @param operatorId 操作人
     */
    @Override
    public void save(String operatorId) {
        // 1. 初始化对象
        this.initialize(operatorId);
        // 2. 领域规则校验
        Assert.notBlank(this.name, "凭证名称不能为空");
        // 3. 赋值
        if (this.enabled == null) {
            this.enabled = Boolean.TRUE;
        }
        if (StrUtil.isBlank(this.num)) {
            this.num = this.accessGateway.generateCredentialNum();
            this.rawSecret = this.accessGateway.generateApiSecret();
            this.keyPrefix = StrUtil.sub(this.rawSecret, 0, Math.min(8, this.rawSecret.length()));
            this.secretHash = this.accessGateway.hashSecret(this.rawSecret);
        }
        // 4. 领域完整性校验
        this.validate();
        // 5. 持久化对象
        this.credentialRepository.save(this);
        // 6. 发布领域事件
        this.sendEvent(DomainEventConstant.CREDENTIAL_CREATED, operatorId);
    }

    /**
     * 删除凭证。
     *
     * @param operatorId 操作人
     */
    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.num, "凭证编号不能为空");
        this.validate();
        this.credentialRepository.deleteByNum(this.num);
        this.sendEvent(DomainEventConstant.CREDENTIAL_DELETED, operatorId);
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.name, "凭证名称不能为空");
        Assert.notBlank(this.secretHash, "密钥哈希不能为空");
        Assert.notNull(this.enabled, "启用状态不能为空");
    }

    /**
     * 停用凭证。
     *
     * @param operatorId 操作人
     */
    public void disable(String operatorId) {
        this.initialize(operatorId);
        this.enabled = Boolean.FALSE;
        this.validate();
        this.credentialRepository.save(this);
        this.sendEvent(DomainEventConstant.CREDENTIAL_DISABLED, operatorId);
    }

    /**
     * 校验明文密钥。
     *
     * @param rawSecret  明文密钥
     * @param operatorId 操作人
     */
    public void assertSecret(String rawSecret, String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue(Boolean.TRUE.equals(this.enabled), "凭证已停用");
        Assert.notBlank(rawSecret, "密钥不能为空");
        if (!this.accessGateway.matchesSecret(rawSecret, this.secretHash)) {
            throw new BusinessException(401, "API Key 无效");
        }
        this.validate();
    }

    private void sendEvent(String type, String operatorId) {
        this.domainEventPublisher.send(DomainEventDTO.builder()
                .id(IdUtil.fastSimpleUUID())
                .type(type)
                .data(this.num)
                .time(System.currentTimeMillis())
                .sender(operatorId)
                .build());
    }
}
