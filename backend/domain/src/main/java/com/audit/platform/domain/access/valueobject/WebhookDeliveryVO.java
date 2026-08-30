package com.audit.platform.domain.access.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookDeliveryVO {
    private Long id;
    private String num;
    private String eventId;
    private String evaluationNum;
    private String bizId;
    private String eventName;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private String callbackUrl;
}
