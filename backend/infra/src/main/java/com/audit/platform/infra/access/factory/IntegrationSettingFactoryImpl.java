package com.audit.platform.infra.access.factory;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.IntegrationSetting;
import com.audit.platform.domain.access.factory.IntegrationSettingFactory;
import com.audit.platform.domain.access.gateway.AccessGateway;
import com.audit.platform.domain.access.repository.IntegrationSettingRepository;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class IntegrationSettingFactoryImpl implements IntegrationSettingFactory {

    @Resource
    private IntegrationSettingRepository integrationSettingRepository;
    @Resource
    private AccessGateway accessGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public IntegrationSetting create(String callbackUrl, String subscribedEventsCsv, BigDecimal classifyThreshold) {
        IntegrationSetting setting = new IntegrationSetting(callbackUrl, subscribedEventsCsv, classifyThreshold,
                this.integrationSettingRepository, this.accessGateway, this.domainEventPublisher);
        setting.setNum(IntegrationSetting.DEFAULT_NUM);
        return setting;
    }

    @Override
    public IntegrationSetting createByNum(String num) {
        IntegrationSetting setting = this.integrationSettingRepository.findByNum(
                StrUtil.blankToDefault(num, IntegrationSetting.DEFAULT_NUM));
        if (setting == null) {
            return create("", "evaluation.scored,evaluation.finalized,evaluation.failed", new BigDecimal("0.7000"));
        }
        setting.setIntegrationSettingRepository(this.integrationSettingRepository);
        setting.setAccessGateway(this.accessGateway);
        setting.setDomainEventPublisher(this.domainEventPublisher);
        return setting;
    }
}
