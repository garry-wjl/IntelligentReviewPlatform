package com.audit.platform.application.ruleset;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.application.common.PageQueryHelper;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.ruleset.dto.RuleItemDTO;
import com.audit.platform.client.ruleset.dto.RuleSetDTO;
import com.audit.platform.client.ruleset.dto.RuleSetDetailDTO;
import com.audit.platform.client.ruleset.dto.RuleSetNumParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetPageParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetVersionDTO;
import com.audit.platform.client.ruleset.dto.RuleSetVersionNumParamDTO;
import com.audit.platform.client.scene.dto.SceneParamDTO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.domain.scene.valueobject.SceneParamVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.ruleset.entity.RuleItemEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetVersionEntity;
import com.audit.platform.infra.ruleset.mapper.RuleItemMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleSetQueryService {
    @Resource
    private RuleSetMapper ruleSetMapper;
    @Resource
    private RuleSetVersionMapper ruleSetVersionMapper;
    @Resource
    private RuleItemMapper ruleItemMapper;

    public PageDTO<RuleSetDTO> page(RuleSetPageParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<RuleSetEntity> wrapper = new LambdaQueryWrapper<>();
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), param.getName(), param.getKeyword(),
                RuleSetEntity::getNum, RuleSetEntity::getName);
        wrapper.eq(param.getEnabled() != null, RuleSetEntity::getEnabled, param.getEnabled());
        wrapper.orderByDesc(RuleSetEntity::getUpdateTime);
        Page<RuleSetEntity> page = ruleSetMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<RuleSetDTO> list = new ArrayList<>();
        for (RuleSetEntity entity : page.getRecords()) {
            RuleSetDTO dto = BeanUtil.copyProperties(entity, RuleSetDTO.class);
            RuleSetVersionEntity current = currentVersion(entity.getCurrentPublishedVersionNum());
            if (current != null) {
                dto.setCurrentVersionNo(current.getVersionNo());
                dto.setRuleCount(countRules(current.getNum()));
            }
            list.add(dto);
        }
        return PageDTO.<RuleSetDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public RuleSetDetailDTO detail(RuleSetNumParamDTO param) {
        RuleSetEntity entity = ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, param.getNum()));
        if (entity == null) {
            throw new BusinessException("规则集不存在");
        }
        RuleSetDetailDTO dto = BeanUtil.copyProperties(entity, RuleSetDetailDTO.class);
        List<SceneParamDTO> sceneParams = new ArrayList<>();
        for (SceneParamVO sceneParam : SceneParamCatalog.resolveFromResolvedJson(entity.getSceneParamsJson())) {
            sceneParams.add(SceneParamDTO.builder()
                    .key(sceneParam.getKey())
                    .label(sceneParam.getLabel())
                    .type(sceneParam.getType())
                    .builtin(sceneParam.getBuiltin())
                    .build());
        }
        dto.setSceneParams(sceneParams);
        List<RuleSetVersionEntity> versions = ruleSetVersionMapper.selectList(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getRuleSetNum, entity.getNum())
                .orderByDesc(RuleSetVersionEntity::getId));
        List<RuleSetVersionDTO> versionDtos = new ArrayList<>();
        for (RuleSetVersionEntity version : versions) {
            RuleSetVersionDTO item = BeanUtil.copyProperties(version, RuleSetVersionDTO.class);
            item.setRuleSetNum(entity.getNum());
            versionDtos.add(item);
        }
        dto.setVersions(versionDtos);
        RuleSetVersionEntity current = currentVersion(entity.getCurrentPublishedVersionNum());
        if (current != null) {
            dto.setCurrentVersionNo(current.getVersionNo());
        }
        return dto;
    }

    public RuleSetVersionDTO versionDetail(RuleSetVersionNumParamDTO param) {
        RuleSetVersionEntity version = ruleSetVersionMapper.selectOne(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getNum, param.getVersionNum()));
        if (version == null) {
            throw new BusinessException("规则集版本不存在");
        }
        RuleSetVersionDTO dto = BeanUtil.copyProperties(version, RuleSetVersionDTO.class);
        List<RuleItemEntity> rules = ruleItemMapper.selectList(new LambdaQueryWrapper<RuleItemEntity>()
                .eq(RuleItemEntity::getVersionNum, version.getNum())
                .orderByAsc(RuleItemEntity::getSortNo));
        List<RuleItemDTO> ruleDtos = new ArrayList<>();
        for (RuleItemEntity rule : rules) {
            RuleItemDTO item = BeanUtil.copyProperties(rule, RuleItemDTO.class);
            RuleEngineConfigHelper.fill(item, rule.getEngineKind(), rule.getEngineConfigJson());
            ruleDtos.add(item);
        }
        dto.setRules(ruleDtos);
        return dto;
    }

    public void requirePublished(RuleSetNumParamDTO param) {
        RuleSetEntity entity = ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, param.getNum()));
        if (entity == null || StrUtil.isBlank(entity.getCurrentPublishedVersionNum())) {
            throw new BusinessException("规则集未发布");
        }
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BusinessException("规则集已停用");
        }
    }

    private RuleSetVersionEntity currentVersion(String versionNum) {
        if (StrUtil.isBlank(versionNum)) {
            return null;
        }
        return ruleSetVersionMapper.selectOne(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getNum, versionNum));
    }

    private int countRules(String versionNum) {
        Long count = ruleItemMapper.selectCount(new LambdaQueryWrapper<RuleItemEntity>()
                .eq(RuleItemEntity::getVersionNum, versionNum));
        return count == null ? 0 : count.intValue();
    }
}
