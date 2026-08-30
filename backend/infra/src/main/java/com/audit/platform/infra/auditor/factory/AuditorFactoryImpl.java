package com.audit.platform.infra.auditor.factory;

import com.audit.platform.domain.auditor.Auditor;
import com.audit.platform.domain.auditor.factory.AuditorFactory;
import com.audit.platform.domain.auditor.gateway.AuditorGateway;
import com.audit.platform.domain.auditor.repository.AuditorRepository;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AuditorFactoryImpl implements AuditorFactory {

    @Resource
    private AuditorRepository auditorRepository;
    @Resource
    private AuditorGateway auditorGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public Auditor create(String name, String kind, String agentNum, String description) {
        return new Auditor(name, kind, agentNum, description, this.auditorRepository, this.auditorGateway,
                this.domainEventPublisher);
    }

    @Override
    public Auditor createByNum(String num) {
        Auditor auditor = this.auditorRepository.findByNum(num);
        if (auditor == null) {
            return null;
        }
        auditor.setAuditorRepository(this.auditorRepository);
        auditor.setAuditorGateway(this.auditorGateway);
        auditor.setDomainEventPublisher(this.domainEventPublisher);
        return auditor;
    }
}
