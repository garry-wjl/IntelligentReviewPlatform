package com.audit.platform.infra.auditor.mapper;

import com.audit.platform.infra.auditor.entity.AuditorEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditorMapper extends BaseMapper<AuditorEntity> {
}
