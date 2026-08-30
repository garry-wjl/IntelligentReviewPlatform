package com.audit.platform.infra.ruleset.gateway;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.ruleset.gateway.RuleSetGateway;
import com.audit.platform.domain.ruleset.valueobject.AuditorSnapshotVO;
import com.audit.platform.domain.ruleset.valueobject.SceneSnapshotVO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.auditor.entity.AuditorEntity;
import com.audit.platform.infra.auditor.mapper.AuditorMapper;
import com.audit.platform.infra.common.util.NumGenerator;
import com.audit.platform.infra.scene.entity.SceneEntity;
import com.audit.platform.infra.scene.mapper.SceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuleSetGatewayImpl implements RuleSetGateway {
    @Resource
    private NumGenerator numGenerator;
    @Resource
    private SceneMapper sceneMapper;
    @Resource
    private AuditorMapper auditorMapper;

    @Override
    public String generateNum() {
        return numGenerator.next("RS-", 4);
    }

    @Override
    public String generateVersionNum() {
        return numGenerator.next("RSV-", 4);
    }

    @Override
    public String generateRuleNum() {
        return numGenerator.next("RUL-", 4);
    }

    @Override
    public SceneSnapshotVO loadScene(String sceneNum) {
        if (StrUtil.isBlank(sceneNum)) {
            throw new BusinessException("必须选择场景");
        }
        SceneEntity entity = sceneMapper.selectOne(new LambdaQueryWrapper<SceneEntity>()
                .eq(SceneEntity::getNum, sceneNum));
        if (entity == null) {
            throw new BusinessException("场景不存在");
        }
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BusinessException("场景已停用");
        }
        return SceneSnapshotVO.builder()
                .sceneNum(entity.getNum())
                .sceneName(entity.getName())
                .paramsJson(SceneParamCatalog.toResolvedJson(entity.getExtraParamsJson()))
                .build();
    }

    @Override
    public AuditorSnapshotVO loadAuditor(String auditorNum) {
        if (StrUtil.isBlank(auditorNum)) {
            throw new BusinessException("必须选择审核器");
        }
        AuditorEntity entity = auditorMapper.selectOne(new LambdaQueryWrapper<AuditorEntity>()
                .eq(AuditorEntity::getNum, auditorNum));
        if (entity == null) {
            throw new BusinessException("审核器不存在");
        }
        return AuditorSnapshotVO.builder()
                .num(entity.getNum())
                .name(entity.getName())
                .kind(entity.getKind())
                .agentName(entity.getAgentName())
                .enabled(entity.getEnabled())
                .build();
    }
}
