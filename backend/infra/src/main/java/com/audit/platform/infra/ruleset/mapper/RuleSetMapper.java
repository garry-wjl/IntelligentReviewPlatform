package com.audit.platform.infra.ruleset.mapper;

import com.audit.platform.infra.ruleset.entity.RuleSetEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RuleSetMapper extends BaseMapper<RuleSetEntity> {
}
