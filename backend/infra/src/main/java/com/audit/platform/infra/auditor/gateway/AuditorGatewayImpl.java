package com.audit.platform.infra.auditor.gateway;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.auditor.gateway.AuditorGateway;
import com.audit.platform.domain.auditor.valueobject.AgentOptionVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.auditor.entity.AgentCatalogEntity;
import com.audit.platform.infra.auditor.mapper.AgentCatalogMapper;
import com.audit.platform.infra.common.client.MockAgentPlatformClient;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
import com.audit.platform.infra.common.util.NumGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuditorGatewayImpl implements AuditorGateway {

    @Resource
    private NumGenerator numGenerator;
    @Resource
    private MockAgentPlatformClient mockAgentPlatformClient;
    @Resource
    private AgentCatalogMapper agentCatalogMapper;

    @Override
    public String generateNum() {
        return this.numGenerator.next("AUD-", 4);
    }

    @Override
    public List<AgentOptionVO> fetchAgents() {
        return this.mockAgentPlatformClient.listAgents();
    }

    @Override
    public String resolveAgentName(String agentNum) {
        if (StrUtil.isBlank(agentNum)) {
            throw new BusinessException("Agent 编码不能为空");
        }
        AgentCatalogEntity entity = this.agentCatalogMapper.selectOne(
                new LambdaQueryWrapper<AgentCatalogEntity>().eq(AgentCatalogEntity::getAgentNum, agentNum));
        if (entity == null) {
            throw new BusinessException("Agent 不存在");
        }
        return entity.getName();
    }

    @Override
    public void persistAgentCatalog(List<AgentOptionVO> agents) {
        if (CollUtil.isEmpty(agents)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (AgentOptionVO agent : agents) {
            AgentCatalogEntity entity = new AgentCatalogEntity();
            entity.setAgentNum(agent.getAgentNum());
            entity.setName(agent.getName());
            entity.setDescription(agent.getDescription() == null ? "" : agent.getDescription());
            entity.setProvider(agent.getProvider() == null ? "" : agent.getProvider());
            entity.setUpdateId("system");
            entity.setUpdateTime(now);
            entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
            AgentCatalogEntity raw = this.agentCatalogMapper.selectRawByAgentNum(agent.getAgentNum());
            if (raw != null) {
                this.agentCatalogMapper.restoreById(raw.getId());
                entity.setId(raw.getId());
                entity.setNum(raw.getNum());
                entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), "system"));
                entity.setCreateTime(raw.getCreateTime() == null ? now : raw.getCreateTime());
                this.agentCatalogMapper.updateById(entity);
            } else {
                entity.setNum(this.numGenerator.next("AGC-", 4));
                entity.setCreateId("system");
                entity.setCreateTime(now);
                this.agentCatalogMapper.insert(entity);
            }
        }
    }
}
