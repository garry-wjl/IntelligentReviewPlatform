package com.audit.platform.infra.ruleset.repository;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.audit.platform.domain.ruleset.RuleSet;
import com.audit.platform.domain.ruleset.repository.RuleSetRepository;
import com.audit.platform.domain.ruleset.valueobject.RuleItemVO;
import com.audit.platform.domain.ruleset.valueobject.RuleSetVersionVO;
import com.audit.platform.infra.ruleset.entity.RuleItemEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetVersionEntity;
import com.audit.platform.infra.ruleset.mapper.RuleItemMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class RuleSetRepositoryImpl implements RuleSetRepository {
    @Resource
    private RuleSetMapper ruleSetMapper;
    @Resource
    private RuleSetVersionMapper ruleSetVersionMapper;
    @Resource
    private RuleItemMapper ruleItemMapper;

    @Override
    public void save(RuleSet aggregate) {
        RuleSetEntity entity = BeanUtil.copyProperties(aggregate, RuleSetEntity.class);
        RuleSetEntity existed = ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, aggregate.getNum()));
        if (existed == null) {
            ruleSetMapper.insert(entity);
        } else {
            entity.setId(existed.getId());
            ruleSetMapper.updateById(entity);
        }
        Set<String> keepVersions = new HashSet<>();
        if (CollUtil.isNotEmpty(aggregate.getVersions())) {
            for (RuleSetVersionVO version : aggregate.getVersions()) {
                keepVersions.add(version.getNum());
                upsertVersion(aggregate, version);
            }
        }
        List<RuleSetVersionEntity> oldVersions = ruleSetVersionMapper.selectList(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getRuleSetNum, aggregate.getNum()));
        for (RuleSetVersionEntity old : oldVersions) {
            if (!keepVersions.contains(old.getNum())) {
                ruleSetVersionMapper.deleteById(old.getId());
            }
        }
    }

    @Override
    public RuleSet findByNum(String num) {
        RuleSetEntity entity = ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, num));
        if (entity == null) {
            return null;
        }
        RuleSet ruleSet = new RuleSet();
        BeanUtil.copyProperties(entity, ruleSet);
        ruleSet.setRuleSetRepository(this);
        List<RuleSetVersionEntity> versions = ruleSetVersionMapper.selectList(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getRuleSetNum, num));
        List<RuleSetVersionVO> versionVos = new ArrayList<>();
        for (RuleSetVersionEntity version : versions) {
            RuleSetVersionVO vo = BeanUtil.copyProperties(version, RuleSetVersionVO.class);
            List<RuleItemEntity> rules = ruleItemMapper.selectList(new LambdaQueryWrapper<RuleItemEntity>()
                    .eq(RuleItemEntity::getVersionNum, version.getNum())
                    .orderByAsc(RuleItemEntity::getSortNo));
            List<RuleItemVO> ruleVos = new ArrayList<>();
            for (RuleItemEntity rule : rules) {
                ruleVos.add(BeanUtil.copyProperties(rule, RuleItemVO.class));
            }
            vo.setRules(ruleVos);
            versionVos.add(vo);
        }
        ruleSet.setVersions(versionVos);
        return ruleSet;
    }

    @Override
    public void deleteByNum(String num) {
        ruleSetMapper.delete(new LambdaQueryWrapper<RuleSetEntity>().eq(RuleSetEntity::getNum, num));
    }

    private void upsertVersion(RuleSet aggregate, RuleSetVersionVO version) {
        RuleSetVersionEntity versionEntity = BeanUtil.copyProperties(version, RuleSetVersionEntity.class);
        versionEntity.setRuleSetNum(aggregate.getNum());
        versionEntity.setCreateId(aggregate.getCreateId());
        versionEntity.setUpdateId(aggregate.getUpdateId());
        RuleSetVersionEntity found = ruleSetVersionMapper.selectOne(new LambdaQueryWrapper<RuleSetVersionEntity>()
                .eq(RuleSetVersionEntity::getNum, version.getNum()));
        if (found == null) {
            ruleSetVersionMapper.insert(versionEntity);
        } else {
            versionEntity.setId(found.getId());
            ruleSetVersionMapper.updateById(versionEntity);
        }
        Set<String> keepRules = new HashSet<>();
        if (CollUtil.isNotEmpty(version.getRules())) {
            for (RuleItemVO rule : version.getRules()) {
                keepRules.add(rule.getNum());
                RuleItemEntity ruleEntity = BeanUtil.copyProperties(rule, RuleItemEntity.class);
                ruleEntity.setVersionNum(version.getNum());
                ruleEntity.setCreateId(aggregate.getCreateId());
                ruleEntity.setUpdateId(aggregate.getUpdateId());
                RuleItemEntity existedRule = ruleItemMapper.selectOne(new LambdaQueryWrapper<RuleItemEntity>()
                        .eq(RuleItemEntity::getNum, rule.getNum()));
                if (existedRule == null) {
                    ruleItemMapper.insert(ruleEntity);
                } else {
                    ruleEntity.setId(existedRule.getId());
                    ruleItemMapper.updateById(ruleEntity);
                }
            }
        }
        List<RuleItemEntity> oldRules = ruleItemMapper.selectList(new LambdaQueryWrapper<RuleItemEntity>()
                .eq(RuleItemEntity::getVersionNum, version.getNum()));
        for (RuleItemEntity old : oldRules) {
            if (!keepRules.contains(old.getNum())) {
                ruleItemMapper.deleteById(old.getId());
            }
        }
    }
}
