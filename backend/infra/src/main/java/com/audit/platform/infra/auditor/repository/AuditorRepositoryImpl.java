package com.audit.platform.infra.auditor.repository;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.auditor.Auditor;
import com.audit.platform.domain.auditor.repository.AuditorRepository;
import com.audit.platform.infra.auditor.entity.AuditorEntity;
import com.audit.platform.infra.auditor.mapper.AuditorMapper;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class AuditorRepositoryImpl implements AuditorRepository {

    @Resource
    private AuditorMapper auditorMapper;

    @Override
    public void save(Auditor aggregate) {
        AuditorEntity existed = selectByNum(aggregate.getNum());
        AuditorEntity entity = toEntity(aggregate);
        if (existed != null) {
            entity.setId(existed.getId());
            this.auditorMapper.updateById(entity);
        } else {
            this.auditorMapper.insert(entity);
        }
    }

    @Override
    public Auditor findByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return null;
        }
        AuditorEntity entity = selectByNum(num);
        if (entity == null) {
            return null;
        }
        Auditor domain = new Auditor();
        domain.setId(entity.getId());
        domain.setNum(entity.getNum());
        domain.setName(entity.getName());
        domain.setKind(entity.getKind());
        domain.setAgentNum(entity.getAgentNum());
        domain.setAgentName(entity.getAgentName());
        domain.setDescription(entity.getDescription());
        domain.setEnabled(entity.getEnabled());
        domain.setCreateId(entity.getCreateId());
        domain.setUpdateId(entity.getUpdateId());
        domain.setCreateTime(entity.getCreateTime());
        domain.setUpdateTime(entity.getUpdateTime());
        domain.setAuditorRepository(this);
        return domain;
    }

    @Override
    public void deleteByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return;
        }
        this.auditorMapper.delete(new LambdaQueryWrapper<AuditorEntity>().eq(AuditorEntity::getNum, num));
    }

    private AuditorEntity selectByNum(String num) {
        return this.auditorMapper.selectOne(new LambdaQueryWrapper<AuditorEntity>().eq(AuditorEntity::getNum, num));
    }

    private AuditorEntity toEntity(Auditor aggregate) {
        AuditorEntity entity = new AuditorEntity();
        entity.setNum(aggregate.getNum());
        entity.setName(aggregate.getName());
        entity.setKind(aggregate.getKind());
        entity.setAgentNum(aggregate.getAgentNum());
        entity.setAgentName(aggregate.getAgentName());
        entity.setDescription(aggregate.getDescription());
        entity.setEnabled(aggregate.getEnabled());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        return entity;
    }
}
