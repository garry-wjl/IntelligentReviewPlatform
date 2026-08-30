package com.audit.platform.infra.common.client;

import com.audit.platform.domain.auditor.valueobject.AgentOptionVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock Agent 平台，目录与初始化 DML 一致。
 */
@Component
public class MockAgentPlatformClient {

    public List<AgentOptionVO> listAgents() {
        return List.of(
                AgentOptionVO.builder()
                        .agentNum("agent-quality")
                        .name("文档质量审核 Agent")
                        .description("擅长报告结构、论证充分性与表述质量判断。")
                        .provider("Agent 平台")
                        .build(),
                AgentOptionVO.builder()
                        .agentNum("agent-risk")
                        .name("风险合规审核 Agent")
                        .description("侧重风险披露、合规条款与红线项识别。")
                        .provider("Agent 平台")
                        .build(),
                AgentOptionVO.builder()
                        .agentNum("agent-general")
                        .name("通用文本审核 Agent")
                        .description("通用多文档理解与逐条打分。")
                        .provider("Agent 平台")
                        .build()
        );
    }
}
