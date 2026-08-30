package com.audit.platform.facade.domain;

/**
 * Domain event publisher interface, implemented by infra.
 */
public interface DomainEventPublisher {
    void send(DomainEventDTO eventDTO);
}
