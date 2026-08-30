package com.audit.platform.domain.access.repository;

import com.audit.platform.domain.access.IntegrationSetting;

public interface IntegrationSettingRepository {
    void save(IntegrationSetting aggregate);

    IntegrationSetting findByNum(String num);

    void deleteByNum(String num);
}
