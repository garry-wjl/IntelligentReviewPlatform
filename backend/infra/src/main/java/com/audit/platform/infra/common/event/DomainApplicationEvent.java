package com.audit.platform.infra.common.event;

import com.audit.platform.facade.domain.DomainEventDTO;
import org.springframework.context.ApplicationEvent;

/**
 * 将领域事件包装为 Spring ApplicationEvent。
 */
public class DomainApplicationEvent extends ApplicationEvent {
    private final DomainEventDTO domainEvent;

    public DomainApplicationEvent(DomainEventDTO domainEvent) {
        super(domainEvent);
        this.domainEvent = domainEvent;
    }

    public DomainEventDTO getDomainEvent() {
        return domainEvent;
    }
}
