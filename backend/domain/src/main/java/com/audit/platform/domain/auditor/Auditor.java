package com.audit.platform.domain.auditor;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.auditor.gateway.AuditorGateway;
import com.audit.platform.domain.auditor.repository.AuditorRepository;
import com.audit.platform.domain.auditor.valueobject.AgentOptionVO;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 审核器聚合根。
 */
@Getter
@Setter
public class Auditor extends DomainEntity {
    private String num;
    private String name;
    private String kind;
    private String agentNum;
    private String agentName;
    private String description;
    private Boolean enabled;

    private AuditorRepository auditorRepository;
    private AuditorGateway auditorGateway;
    private DomainEventPublisher domainEventPublisher;

    public Auditor() {
    }

    public Auditor(String name, String kind, String agentNum, String description,
                   AuditorRepository auditorRepository, AuditorGateway auditorGateway,
                   DomainEventPublisher domainEventPublisher) {
        this.name = name;
        this.kind = kind;
        this.agentNum = agentNum;
        this.description = description;
        this.auditorRepository = auditorRepository;
        this.auditorGateway = auditorGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void save(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.name, "审核器名称不能为空");
        Assert.notBlank(this.kind, "审核器类型不能为空");
        if (this.enabled == null) {
            this.enabled = Boolean.TRUE;
        }
        if (this.description == null) {
            this.description = "";
        }
        bindAgent();
        if (StrUtil.isBlank(this.num)) {
            this.num = this.auditorGateway.generateNum();
        }
        this.validate();
        this.auditorRepository.save(this);
        this.sendEvent(DomainEventConstant.AUDITOR_SAVED, operatorId);
    }

    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.num, "审核器编号不能为空");
        this.validate();
        this.auditorRepository.deleteByNum(this.num);
        this.sendEvent(DomainEventConstant.AUDITOR_DELETED, operatorId);
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.name, "审核器名称不能为空");
        Assert.isTrue("AGENT".equals(this.kind) || "ORDINARY".equals(this.kind), "审核器类型不合法");
        if ("AGENT".equals(this.kind)) {
            Assert.notBlank(this.agentNum, "Agent 审核器必须选择 Agent");
        }
    }

    /**
     * 更新定义。
     */
    public void updateProfile(String name, String kind, String agentNum, String description, String operatorId) {
        this.initialize(operatorId);
        this.name = name;
        this.kind = kind;
        this.agentNum = agentNum;
        this.description = description == null ? "" : description;
        bindAgent();
        this.validate();
        this.auditorRepository.save(this);
        this.sendEvent(DomainEventConstant.AUDITOR_SAVED, operatorId);
    }

    /**
     * 启停。
     */
    public void setEnabledFlag(Boolean enabled, String operatorId) {
        this.initialize(operatorId);
        Assert.notNull(enabled, "启用状态不能为空");
        this.enabled = enabled;
        this.validate();
        this.auditorRepository.save(this);
        this.sendEvent(DomainEventConstant.AUDITOR_ENABLED_CHANGED, operatorId);
    }

    /**
     * 从 Agent 平台刷新可选目录。
     */
    public void refreshAgentCatalog(String operatorId) {
        this.initialize(operatorId);
        List<AgentOptionVO> agents = this.auditorGateway.fetchAgents();
        this.auditorGateway.persistAgentCatalog(agents);
        this.validate();
        this.auditorRepository.save(this);
        this.sendEvent(DomainEventConstant.AGENT_CATALOG_REFRESHED, operatorId);
    }

    private void bindAgent() {
        if ("ORDINARY".equals(this.kind)) {
            this.agentNum = null;
            this.agentName = null;
            return;
        }
        this.agentName = this.auditorGateway.resolveAgentName(this.agentNum);
    }

    private void sendEvent(String type, String operatorId) {
        this.domainEventPublisher.send(DomainEventDTO.builder()
                .id(IdUtil.fastSimpleUUID())
                .type(type)
                .data(this.num)
                .time(System.currentTimeMillis())
                .sender(operatorId)
                .build());
    }
}
