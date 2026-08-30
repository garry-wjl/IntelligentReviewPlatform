package com.audit.platform.domain.access.factory;

import com.audit.platform.domain.access.IntegrationSetting;

import java.math.BigDecimal;

public interface IntegrationSettingFactory {
    /**
     * 按回调配置构建接入设置。
     *
     * @param callbackUrl          回调地址
     * @param subscribedEventsCsv  订阅事件
     * @param classifyThreshold    识别阈值
     * @return 接入设置
     */
    IntegrationSetting create(String callbackUrl, String subscribedEventsCsv, BigDecimal classifyThreshold);

    /**
     * 按业务编码加载，单租户固定 {@code INT-DEFAULT}。
     *
     * @param num 设置编号
     * @return 接入设置
     */
    IntegrationSetting createByNum(String num);
}
