package com.audit.platform.application.access;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.application.common.PageQueryHelper;
import com.audit.platform.client.access.dto.CredentialDTO;
import com.audit.platform.client.access.dto.CredentialPageParamDTO;
import com.audit.platform.client.access.dto.IntegrationDTO;
import com.audit.platform.client.access.dto.WebhookLogDTO;
import com.audit.platform.client.access.dto.WebhookLogQueryParamDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.domain.access.IntegrationSetting;
import com.audit.platform.infra.access.entity.CredentialEntity;
import com.audit.platform.infra.access.entity.IntegrationSettingEntity;
import com.audit.platform.infra.access.entity.WebhookDeliveryEntity;
import com.audit.platform.infra.access.mapper.CredentialMapper;
import com.audit.platform.infra.access.mapper.IntegrationSettingMapper;
import com.audit.platform.infra.access.mapper.WebhookDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccessQueryService {
    @Resource
    private CredentialMapper credentialMapper;
    @Resource
    private IntegrationSettingMapper integrationSettingMapper;
    @Resource
    private WebhookDeliveryMapper webhookDeliveryMapper;

    public PageDTO<CredentialDTO> pageCredentials(CredentialPageParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<CredentialEntity> wrapper = new LambdaQueryWrapper<>();
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), param.getName(), param.getKeyword(),
                CredentialEntity::getNum, CredentialEntity::getName);
        wrapper.orderByDesc(CredentialEntity::getCreateTime);
        Page<CredentialEntity> page = credentialMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<CredentialDTO> list = new ArrayList<>();
        for (CredentialEntity entity : page.getRecords()) {
            list.add(BeanUtil.copyProperties(entity, CredentialDTO.class));
        }
        return PageDTO.<CredentialDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public IntegrationDTO getIntegration(EmptyParamDTO param) {
        IntegrationSettingEntity entity = integrationSettingMapper.selectOne(
                new LambdaQueryWrapper<IntegrationSettingEntity>()
                        .eq(IntegrationSettingEntity::getNum, IntegrationSetting.DEFAULT_NUM));
        if (entity == null) {
            return IntegrationDTO.builder()
                    .num(IntegrationSetting.DEFAULT_NUM)
                    .callbackUrl("")
                    .subscribedEvents("evaluation.scored,evaluation.finalized,evaluation.failed")
                    .classifyThreshold(new java.math.BigDecimal("0.7000"))
                    .build();
        }
        return BeanUtil.copyProperties(entity, IntegrationDTO.class);
    }

    public PageDTO<WebhookLogDTO> pageWebhookLogs(WebhookLogQueryParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<WebhookDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), param.getName(), param.getKeyword(),
                WebhookDeliveryEntity::getNum, WebhookDeliveryEntity::getBizId);
        wrapper.eq(StrUtil.isNotBlank(param.getEvaluationNum()),
                WebhookDeliveryEntity::getEvaluationNum, param.getEvaluationNum());
        wrapper.eq(StrUtil.isNotBlank(param.getStatus()),
                WebhookDeliveryEntity::getStatus, param.getStatus());
        wrapper.orderByDesc(WebhookDeliveryEntity::getCreateTime);
        Page<WebhookDeliveryEntity> page = webhookDeliveryMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<WebhookLogDTO> list = new ArrayList<>();
        for (WebhookDeliveryEntity entity : page.getRecords()) {
            list.add(BeanUtil.copyProperties(entity, WebhookLogDTO.class));
        }
        return PageDTO.<WebhookLogDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public List<String> listDueDeliveryNums(EmptyParamDTO param) {
        List<WebhookDeliveryEntity> rows = webhookDeliveryMapper.selectList(new LambdaQueryWrapper<WebhookDeliveryEntity>()
                .in(WebhookDeliveryEntity::getStatus, List.of("PENDING", "RETRY"))
                .le(WebhookDeliveryEntity::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(WebhookDeliveryEntity::getNextRetryTime)
                .last("limit 20"));
        List<String> nums = new ArrayList<>();
        for (WebhookDeliveryEntity row : rows) {
            nums.add(row.getNum());
        }
        return nums;
    }
}
