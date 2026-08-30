package com.audit.platform.domain.access.gateway;

import com.audit.platform.domain.access.valueobject.HttpPostResultVO;
import com.audit.platform.domain.access.valueobject.PresignVO;
import com.audit.platform.domain.access.valueobject.WebhookDeliveryVO;

import java.util.List;

public interface AccessGateway {
    String generateCredentialNum();

    String generateApiSecret();

    String hashSecret(String rawSecret);

    boolean matchesSecret(String rawSecret, String secretHash);

    String generateDeliveryNum();

    String generateEventId();

    String findCredentialNumByKeyPrefix(String keyPrefix);

    PresignVO presignPut(String fileName, String contentType);

    HttpPostResultVO httpPost(String url, String payloadJson);

    void enqueueDelayed(String deliveryNum, long delayMs);

    List<String> listDueDeliveryNums(int limit);

    WebhookDeliveryVO loadDelivery(String deliveryNum);
}
