package com.audit.platform.domain.auditor.gateway;

import com.audit.platform.domain.auditor.valueobject.AgentOptionVO;

import java.util.List;

public interface AuditorGateway {
    String generateNum();

    List<AgentOptionVO> fetchAgents();

    String resolveAgentName(String agentNum);

    void persistAgentCatalog(List<AgentOptionVO> agents);
}
