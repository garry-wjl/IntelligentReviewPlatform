package com.audit.platform.infra.common.event;

import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CommonDomainEventPublisher implements DomainEventPublisher {
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void send(DomainEventDTO event) {
        // 发布 PayloadApplicationEvent，供 @TransactionalEventListener(DomainEventDTO) 消费
        this.applicationEventPublisher.publishEvent(event);
    }
}
