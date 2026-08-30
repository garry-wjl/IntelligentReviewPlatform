package com.audit.platform.infra.access.factory;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.Credential;
import com.audit.platform.domain.access.factory.CredentialFactory;
import com.audit.platform.domain.access.gateway.AccessGateway;
import com.audit.platform.domain.access.repository.CredentialRepository;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CredentialFactoryImpl implements CredentialFactory {

    @Resource
    private CredentialRepository credentialRepository;
    @Resource
    private AccessGateway accessGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public Credential create(String name) {
        return new Credential(name, this.credentialRepository, this.accessGateway, this.domainEventPublisher);
    }

    @Override
    public Credential createByNum(String num) {
        Credential credential = this.credentialRepository.findByNum(num);
        if (credential == null) {
            return null;
        }
        credential.setCredentialRepository(this.credentialRepository);
        credential.setAccessGateway(this.accessGateway);
        credential.setDomainEventPublisher(this.domainEventPublisher);
        return credential;
    }

    @Override
    public Credential createByKeyPrefix(String keyPrefix) {
        String num = this.accessGateway.findCredentialNumByKeyPrefix(keyPrefix);
        if (StrUtil.isBlank(num)) {
            return null;
        }
        return createByNum(num);
    }
}
