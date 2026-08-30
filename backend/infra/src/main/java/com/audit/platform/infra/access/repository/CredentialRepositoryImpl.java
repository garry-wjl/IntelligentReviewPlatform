package com.audit.platform.infra.access.repository;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.access.Credential;
import com.audit.platform.domain.access.repository.CredentialRepository;
import com.audit.platform.infra.access.entity.CredentialEntity;
import com.audit.platform.infra.access.mapper.CredentialMapper;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class CredentialRepositoryImpl implements CredentialRepository {

    @Resource
    private CredentialMapper credentialMapper;

    @Override
    public void save(Credential aggregate) {
        CredentialEntity existed = selectByNum(aggregate.getNum());
        CredentialEntity entity = toEntity(aggregate);
        if (existed != null) {
            entity.setId(existed.getId());
            this.credentialMapper.updateById(entity);
        } else {
            this.credentialMapper.insert(entity);
        }
    }

    @Override
    public Credential findByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return null;
        }
        CredentialEntity entity = selectByNum(num);
        if (entity == null) {
            return null;
        }
        Credential domain = new Credential();
        domain.setId(entity.getId());
        domain.setNum(entity.getNum());
        domain.setName(entity.getName());
        domain.setKeyPrefix(entity.getKeyPrefix());
        domain.setSecretHash(entity.getSecretHash());
        domain.setEnabled(entity.getEnabled());
        domain.setCreateId(entity.getCreateId());
        domain.setUpdateId(entity.getUpdateId());
        domain.setCreateTime(entity.getCreateTime());
        domain.setUpdateTime(entity.getUpdateTime());
        domain.setCredentialRepository(this);
        return domain;
    }

    @Override
    public void deleteByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return;
        }
        this.credentialMapper.delete(new LambdaQueryWrapper<CredentialEntity>().eq(CredentialEntity::getNum, num));
    }

    private CredentialEntity selectByNum(String num) {
        return this.credentialMapper.selectOne(new LambdaQueryWrapper<CredentialEntity>().eq(CredentialEntity::getNum, num));
    }

    private CredentialEntity toEntity(Credential aggregate) {
        CredentialEntity entity = new CredentialEntity();
        entity.setNum(aggregate.getNum());
        entity.setName(aggregate.getName());
        entity.setKeyPrefix(aggregate.getKeyPrefix());
        entity.setSecretHash(aggregate.getSecretHash());
        entity.setEnabled(aggregate.getEnabled());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        return entity;
    }
}
