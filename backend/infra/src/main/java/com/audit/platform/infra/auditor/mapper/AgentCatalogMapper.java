package com.audit.platform.infra.auditor.mapper;

import com.audit.platform.infra.auditor.entity.AgentCatalogEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentCatalogMapper extends BaseMapper<AgentCatalogEntity> {

    @Select("SELECT * FROM agent_catalog WHERE agent_num = #{agentNum} LIMIT 1")
    AgentCatalogEntity selectRawByAgentNum(@Param("agentNum") String agentNum);

    @Update("UPDATE agent_catalog SET is_deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
