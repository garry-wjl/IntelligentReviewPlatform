package com.audit.platform.domain.access;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.gateway.AccessGateway;
import com.audit.platform.domain.access.repository.IntegrationSettingRepository;
import com.audit.platform.domain.access.valueobject.HttpPostResultVO;
import com.audit.platform.domain.access.valueobject.PresignVO;
import com.audit.platform.domain.access.valueobject.WebhookDeliveryVO;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import com.audit.platform.facade.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 接入设置聚合根。单租户固定编号 INT-DEFAULT。
 */
@Getter
@Setter
public class IntegrationSetting extends DomainEntity {
    public static final String DEFAULT_NUM = "INT-DEFAULT";

    private String num;
    private String callbackUrl;
    private String subscribedEvents;
    private BigDecimal classifyThreshold;
    /** 当前动作涉及的投递行，不得载入全部历史。 */
    private List<WebhookDeliveryVO> deliveries;

    private IntegrationSettingRepository integrationSettingRepository;
    private AccessGateway accessGateway;
    private DomainEventPublisher domainEventPublisher;

    public IntegrationSetting() {
    }

    public IntegrationSetting(String callbackUrl, String subscribedEvents, BigDecimal classifyThreshold,
                              IntegrationSettingRepository integrationSettingRepository,
                              AccessGateway accessGateway, DomainEventPublisher domainEventPublisher) {
        this.callbackUrl = callbackUrl;
        this.subscribedEvents = subscribedEvents;
        this.classifyThreshold = classifyThreshold;
        this.integrationSettingRepository = integrationSettingRepository;
        this.accessGateway = accessGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 保存接入设置。
     *
     * @param operatorId 操作人
     */
    @Override
    public void save(String operatorId) {
        this.initialize(operatorId);
        if (this.deliveries == null) {
            this.deliveries = new ArrayList<>();
        }
        if (StrUtil.isBlank(this.num)) {
            this.num = DEFAULT_NUM;
        }
        if (this.callbackUrl == null) {
            this.callbackUrl = "";
        }
        if (this.subscribedEvents == null) {
            this.subscribedEvents = "";
        }
        if (this.classifyThreshold == null) {
            this.classifyThreshold = new BigDecimal("0.7000");
        }
        this.validate();
        this.integrationSettingRepository.save(this);
        this.sendEvent(DomainEventConstant.INTEGRATION_UPDATED, operatorId);
    }

    /**
     * 接入设置为单例，禁止删除。
     *
     * @param operatorId 操作人
     */
    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        throw new BusinessException("接入设置不可删除");
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.num, "接入设置编号不能为空");
        Assert.notNull(this.classifyThreshold, "识别阈值不能为空");
    }

    /**
     * 更新回调与订阅。
     *
     * @param callbackUrl       回调地址
     * @param subscribedEvents  订阅事件 CSV
     * @param classifyThreshold 识别阈值
     * @param operatorId        操作人
     */
    public void updateProfile(String callbackUrl, String subscribedEvents, BigDecimal classifyThreshold, String operatorId) {
        this.initialize(operatorId);
        if (StrUtil.isNotBlank(callbackUrl)) {
            Assert.isTrue(StrUtil.startWithAny(callbackUrl, "http://", "https://"), "回调地址必须是 http(s) URL");
        }
        this.callbackUrl = callbackUrl == null ? "" : callbackUrl;
        this.subscribedEvents = subscribedEvents == null ? "" : subscribedEvents;
        this.classifyThreshold = classifyThreshold == null ? new BigDecimal("0.7000") : classifyThreshold;
        this.validate();
        this.integrationSettingRepository.save(this);
        this.sendEvent(DomainEventConstant.INTEGRATION_UPDATED, operatorId);
    }

    /**
     * 签发 TOS 上传预签名。
     *
     * @param fileName    文件名
     * @param contentType MIME
     * @param operatorId  操作人
     * @return 预签名
     */
    public PresignVO issuePresign(String fileName, String contentType, String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(fileName, "文件名不能为空");
        this.validate();
        return this.accessGateway.presignPut(fileName, contentType);
    }

    /**
     * 入队 Webhook。试评或未订阅则跳过。
     *
     * @param evaluationNum    任务编号
     * @param bizId            业务单号
     * @param eventName        事件名
     * @param payloadJson      载荷
     * @param trial            是否试评
     * @param callbackOverride 任务级回调覆盖
     * @param operatorId       操作人
     */
    public void enqueueDelivery(String evaluationNum, String bizId, String eventName, String payloadJson,
                                Boolean trial, String callbackOverride, String operatorId) {
        this.initialize(operatorId);
        if (Boolean.TRUE.equals(trial)) {
            return;
        }
        if (!isSubscribed(eventName)) {
            return;
        }
        String targetUrl = StrUtil.blankToDefault(callbackOverride, this.callbackUrl);
        if (StrUtil.isBlank(targetUrl)) {
            return;
        }
        WebhookDeliveryVO delivery = WebhookDeliveryVO.builder()
                .num(this.accessGateway.generateDeliveryNum())
                .eventId(this.accessGateway.generateEventId())
                .evaluationNum(evaluationNum)
                .bizId(bizId)
                .eventName(eventName)
                .payloadJson(payloadJson)
                .status("PENDING")
                .retryCount(0)
                .nextRetryTime(LocalDateTime.now())
                .callbackUrl(targetUrl)
                .build();
        if (this.deliveries == null) {
            this.deliveries = new ArrayList<>();
        }
        this.deliveries.clear();
        this.deliveries.add(delivery);
        this.validate();
        this.integrationSettingRepository.save(this);
        this.accessGateway.enqueueDelayed(delivery.getNum(), 0L);
        this.sendEvent(DomainEventConstant.WEBHOOK_ENQUEUED, operatorId);
    }

    /**
     * 投递一条到期记录。
     *
     * @param deliveryNum 投递编号
     * @param operatorId  操作人
     */
    public void dispatchOne(String deliveryNum, String operatorId) {
        this.initialize(operatorId);
        WebhookDeliveryVO delivery = this.accessGateway.loadDelivery(deliveryNum);
        if (delivery == null) {
            return;
        }
        if (!"PENDING".equals(delivery.getStatus()) && !"RETRY".equals(delivery.getStatus())) {
            return;
        }
        String url = StrUtil.blankToDefault(delivery.getCallbackUrl(), this.callbackUrl);
        HttpPostResultVO result = this.accessGateway.httpPost(url, delivery.getPayloadJson());
        if (Boolean.TRUE.equals(result.getSuccess())) {
            delivery.setStatus("SUCCESS");
            delivery.setLastError(null);
            persistDelivery(delivery, operatorId, DomainEventConstant.WEBHOOK_SENT);
            return;
        }
        int retry = delivery.getRetryCount() == null ? 0 : delivery.getRetryCount();
        retry = retry + 1;
        delivery.setRetryCount(retry);
        delivery.setLastError(result.getError());
        if (retry >= 8) {
            delivery.setStatus("DEAD");
            persistDelivery(delivery, operatorId, DomainEventConstant.WEBHOOK_FAILED);
            return;
        }
        long delayMs = (long) Math.pow(2, retry) * 1000L;
        delivery.setStatus("RETRY");
        delivery.setNextRetryTime(LocalDateTime.now().plusSeconds(delayMs / 1000));
        persistDelivery(delivery, operatorId, DomainEventConstant.WEBHOOK_FAILED);
        this.accessGateway.enqueueDelayed(delivery.getNum(), delayMs);
    }

    /**
     * 死信重放。
     *
     * @param deliveryNum 投递编号
     * @param operatorId  操作人
     */
    public void replayDead(String deliveryNum, String operatorId) {
        this.initialize(operatorId);
        WebhookDeliveryVO delivery = this.accessGateway.loadDelivery(deliveryNum);
        Assert.notNull(delivery, "投递记录不存在");
        Assert.isTrue("DEAD".equals(delivery.getStatus()), "仅死信可重放");
        delivery.setStatus("PENDING");
        delivery.setRetryCount(0);
        delivery.setNextRetryTime(LocalDateTime.now());
        delivery.setLastError(null);
        persistDelivery(delivery, operatorId, DomainEventConstant.WEBHOOK_ENQUEUED);
        this.accessGateway.enqueueDelayed(delivery.getNum(), 0L);
    }

    private void persistDelivery(WebhookDeliveryVO delivery, String operatorId, String eventType) {
        if (this.deliveries == null) {
            this.deliveries = new ArrayList<>();
        }
        this.deliveries.clear();
        this.deliveries.add(delivery);
        this.validate();
        this.integrationSettingRepository.save(this);
        this.sendEvent(eventType, operatorId);
    }

    private boolean isSubscribed(String eventName) {
        if (StrUtil.isBlank(this.subscribedEvents) || StrUtil.isBlank(eventName)) {
            return false;
        }
        for (String item : StrUtil.split(this.subscribedEvents, ',')) {
            if (eventName.equals(StrUtil.trim(item))) {
                return true;
            }
        }
        return false;
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
