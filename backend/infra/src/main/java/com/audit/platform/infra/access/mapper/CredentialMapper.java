package com.audit.platform.infra.access.mapper;

import com.audit.platform.infra.access.entity.CredentialEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CredentialMapper extends BaseMapper<CredentialEntity> {
}
