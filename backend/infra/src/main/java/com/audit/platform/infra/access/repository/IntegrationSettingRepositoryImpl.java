package com.audit.platform.infra.access.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.IntegrationSetting;
import com.audit.platform.domain.access.repository.IntegrationSettingRepository;
import com.audit.platform.domain.access.valueobject.WebhookDeliveryVO;
import com.audit.platform.infra.access.entity.IntegrationSettingEntity;
import com.audit.platform.infra.access.entity.WebhookDeliveryEntity;
import com.audit.platform.infra.access.mapper.IntegrationSettingMapper;
import com.audit.platform.infra.access.mapper.WebhookDeliveryMapper;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class IntegrationSettingRepositoryImpl implements IntegrationSettingRepository {

    @Resource
    private IntegrationSettingMapper integrationSettingMapper;
    @Resource
    private WebhookDeliveryMapper webhookDeliveryMapper;

    @Override
    public void save(IntegrationSetting aggregate) {
        IntegrationSettingEntity existed = selectByNum(aggregate.getNum());
        IntegrationSettingEntity entity = toEntity(aggregate);
        if (existed != null) {
            entity.setId(existed.getId());
            this.integrationSettingMapper.updateById(entity);
        } else {
            this.integrationSettingMapper.insert(entity);
        }
        if (CollUtil.isNotEmpty(aggregate.getDeliveries())) {
            for (WebhookDeliveryVO delivery : aggregate.getDeliveries()) {
                upsertDelivery(aggregate, delivery);
            }
        }
    }

    @Override
    public IntegrationSetting findByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return null;
        }
        IntegrationSettingEntity entity = selectByNum(num);
        if (entity == null) {
            return null;
        }
        IntegrationSetting domain = new IntegrationSetting();
        domain.setId(entity.getId());
        domain.setNum(entity.getNum());
        domain.setCallbackUrl(entity.getCallbackUrl());
        domain.setSubscribedEvents(entity.getSubscribedEvents());
        domain.setClassifyThreshold(entity.getClassifyThreshold());
        domain.setCreateId(entity.getCreateId());
        domain.setUpdateId(entity.getUpdateId());
        domain.setCreateTime(entity.getCreateTime());
        domain.setUpdateTime(entity.getUpdateTime());
        domain.setIntegrationSettingRepository(this);
        return domain;
    }

    @Override
    public void deleteByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return;
        }
        this.integrationSettingMapper.delete(new LambdaQueryWrapper<IntegrationSettingEntity>()
                .eq(IntegrationSettingEntity::getNum, num));
    }

    private void upsertDelivery(IntegrationSetting aggregate, WebhookDeliveryVO delivery) {
        WebhookDeliveryEntity entity = new WebhookDeliveryEntity();
        entity.setNum(delivery.getNum());
        entity.setEventId(delivery.getEventId());
        entity.setEvaluationNum(delivery.getEvaluationNum());
        entity.setBizId(delivery.getBizId());
        entity.setEventName(delivery.getEventName());
        entity.setPayloadJson(delivery.getPayloadJson());
        entity.setStatus(delivery.getStatus());
        entity.setRetryCount(delivery.getRetryCount());
        entity.setNextRetryTime(delivery.getNextRetryTime());
        entity.setLastError(delivery.getLastError());
        entity.setCallbackUrl(delivery.getCallbackUrl());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        WebhookDeliveryEntity raw = this.webhookDeliveryMapper.selectRawByNum(delivery.getNum());
        if (raw != null) {
            this.webhookDeliveryMapper.restoreById(raw.getId());
            entity.setId(raw.getId());
            entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), aggregate.getCreateId()));
            entity.setCreateTime(raw.getCreateTime() == null ? aggregate.getCreateTime() : raw.getCreateTime());
            this.webhookDeliveryMapper.updateById(entity);
        } else {
            this.webhookDeliveryMapper.insert(entity);
        }
    }

    private IntegrationSettingEntity selectByNum(String num) {
        return this.integrationSettingMapper.selectOne(new LambdaQueryWrapper<IntegrationSettingEntity>()
                .eq(IntegrationSettingEntity::getNum, num));
    }

    private IntegrationSettingEntity toEntity(IntegrationSetting aggregate) {
        IntegrationSettingEntity entity = new IntegrationSettingEntity();
        entity.setNum(aggregate.getNum());
        entity.setCallbackUrl(aggregate.getCallbackUrl());
        entity.setSubscribedEvents(aggregate.getSubscribedEvents());
        entity.setClassifyThreshold(aggregate.getClassifyThreshold());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        return entity;
    }
}
