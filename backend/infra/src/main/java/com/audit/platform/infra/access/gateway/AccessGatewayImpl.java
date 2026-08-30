package com.audit.platform.infra.access.gateway;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.audit.platform.domain.access.gateway.AccessGateway;
import com.audit.platform.domain.access.valueobject.HttpPostResultVO;
import com.audit.platform.domain.access.valueobject.PresignVO;
import com.audit.platform.domain.access.valueobject.WebhookDeliveryVO;
import com.audit.platform.infra.access.entity.CredentialEntity;
import com.audit.platform.infra.access.entity.WebhookDeliveryEntity;
import com.audit.platform.infra.access.mapper.CredentialMapper;
import com.audit.platform.infra.access.mapper.WebhookDeliveryMapper;
import com.audit.platform.infra.common.client.ObjectStorageClient;
import com.audit.platform.infra.common.util.NumGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AccessGatewayImpl implements AccessGateway {
    @Resource
    private NumGenerator numGenerator;
    @Resource
    private CredentialMapper credentialMapper;
    @Resource
    private WebhookDeliveryMapper webhookDeliveryMapper;
    @Resource
    private ObjectStorageClient objectStorageClient;

    @Override
    public String generateCredentialNum() {
        return numGenerator.next("AK-", 4);
    }

    @Override
    public String generateApiSecret() {
        return IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
    }

    @Override
    public String hashSecret(String rawSecret) {
        return DigestUtil.sha256Hex(rawSecret);
    }

    @Override
    public boolean matchesSecret(String rawSecret, String secretHash) {
        return StrUtil.equals(hashSecret(rawSecret), secretHash);
    }

    @Override
    public String generateDeliveryNum() {
        return numGenerator.next("WH-", 6);
    }

    @Override
    public String generateEventId() {
        return "evt-" + IdUtil.fastSimpleUUID();
    }

    @Override
    public String findCredentialNumByKeyPrefix(String keyPrefix) {
        CredentialEntity entity = credentialMapper.selectOne(new LambdaQueryWrapper<CredentialEntity>()
                .eq(CredentialEntity::getKeyPrefix, keyPrefix)
                .last("limit 1"));
        return entity == null ? null : entity.getNum();
    }

    @Override
    public PresignVO presignPut(String fileName, String contentType) {
        String objectKey = "eval/" + java.time.LocalDate.now().toString().replace("-", "") + "/"
                + IdUtil.fastSimpleUUID() + "-" + fileName;
        return PresignVO.builder()
                .objectKey(objectKey)
                .uploadUrl(objectStorageClient.presignPut(objectKey, contentType))
                .method("PUT")
                .build();
    }

    @Override
    public HttpPostResultVO httpPost(String url, String payloadJson) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(StrUtil.blankToDefault(payloadJson, "{}"), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            return HttpPostResultVO.builder()
                    .statusCode(response.statusCode())
                    .success(ok)
                    .error(ok ? null : StrUtil.sub(response.body(), 0, 500))
                    .build();
        } catch (Exception e) {
            return HttpPostResultVO.builder().statusCode(0).success(Boolean.FALSE).error(e.getMessage()).build();
        }
    }

    @Override
    public void enqueueDelayed(String deliveryNum, long delayMs) {
        WebhookDeliveryEntity entity = webhookDeliveryMapper.selectOne(new LambdaQueryWrapper<WebhookDeliveryEntity>()
                .eq(WebhookDeliveryEntity::getNum, deliveryNum));
        if (entity == null) {
            return;
        }
        entity.setNextRetryTime(LocalDateTime.now().plusNanos(delayMs * 1_000_000));
        webhookDeliveryMapper.updateById(entity);
    }

    @Override
    public List<String> listDueDeliveryNums(int limit) {
        List<WebhookDeliveryEntity> rows = webhookDeliveryMapper.selectList(new LambdaQueryWrapper<WebhookDeliveryEntity>()
                .in(WebhookDeliveryEntity::getStatus, List.of("PENDING", "RETRY"))
                .le(WebhookDeliveryEntity::getNextRetryTime, LocalDateTime.now())
                .last("limit " + limit));
        List<String> nums = new ArrayList<>();
        for (WebhookDeliveryEntity row : rows) {
            nums.add(row.getNum());
        }
        return nums;
    }

    @Override
    public WebhookDeliveryVO loadDelivery(String deliveryNum) {
        WebhookDeliveryEntity entity = webhookDeliveryMapper.selectOne(new LambdaQueryWrapper<WebhookDeliveryEntity>()
                .eq(WebhookDeliveryEntity::getNum, deliveryNum));
        if (entity == null) {
            return null;
        }
        return cn.hutool.core.bean.BeanUtil.copyProperties(entity, WebhookDeliveryVO.class);
    }
}
