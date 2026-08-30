package com.audit.platform.facade.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class DomainEventDTO {
    private String id;
    private String type;
    private Object data;
    private Long time;
    private String sender;
}
