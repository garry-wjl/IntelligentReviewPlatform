package com.audit.platform.domain.scene;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.domain.scene.gateway.SceneGateway;
import com.audit.platform.domain.scene.repository.SceneRepository;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.domain.scene.valueobject.SceneParamVO;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 审核场景聚合根。内置 Input / Attachment，可再声明扩展参数。
 */
@Getter
@Setter
public class Scene extends DomainEntity {
    private String num;
    private String name;
    private String description;
    private String extraParamsJson;
    private Boolean enabled;

    private SceneRepository sceneRepository;
    private SceneGateway sceneGateway;
    private DomainEventPublisher domainEventPublisher;

    public Scene() {
    }

    public Scene(String name, String description, String extraParamsJson, SceneRepository sceneRepository,
                 SceneGateway sceneGateway, DomainEventPublisher domainEventPublisher) {
        this.name = name;
        this.description = description;
        this.extraParamsJson = extraParamsJson;
        this.sceneRepository = sceneRepository;
        this.sceneGateway = sceneGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void save(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.name, "场景名称不能为空");
        if (this.enabled == null) {
            this.enabled = Boolean.TRUE;
        }
        if (this.description == null) {
            this.description = "";
        }
        normalizeExtra();
        if (StrUtil.isBlank(this.num)) {
            this.num = this.sceneGateway.generateNum();
        }
        this.validate();
        this.sceneRepository.save(this);
        this.sendEvent(DomainEventConstant.SCENE_SAVED, operatorId);
    }

    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.num, "场景编号不能为空");
        this.validate();
        this.sceneRepository.deleteByNum(this.num);
        this.sendEvent(DomainEventConstant.SCENE_DELETED, operatorId);
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.name, "场景名称不能为空");
        SceneParamCatalog.validateExtra(this.extraParamsJson);
    }

    /**
     * 更新名称、说明与扩展参数。
     */
    public void updateProfile(String name, String description, String extraParamsJson, String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(name, "场景名称不能为空");
        this.name = name;
        this.description = description == null ? "" : description;
        this.extraParamsJson = extraParamsJson;
        normalizeExtra();
        this.validate();
        this.sceneRepository.save(this);
        this.sendEvent(DomainEventConstant.SCENE_SAVED, operatorId);
    }

    /**
     * 启停。
     */
    public void setEnabledFlag(Boolean enabled, String operatorId) {
        this.initialize(operatorId);
        Assert.notNull(enabled, "启用状态不能为空");
        this.enabled = enabled;
        this.validate();
        this.sceneRepository.save(this);
        this.sendEvent(DomainEventConstant.SCENE_ENABLED_CHANGED, operatorId);
    }

    public List<SceneParamVO> resolveParams() {
        return SceneParamCatalog.resolve(this.extraParamsJson);
    }

    private void normalizeExtra() {
        if (StrUtil.isBlank(this.extraParamsJson)) {
            this.extraParamsJson = "[]";
        }
        SceneParamCatalog.validateExtra(this.extraParamsJson);
        this.extraParamsJson = SceneParamCatalog.toExtraJson(SceneParamCatalog.parseExtra(this.extraParamsJson));
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
