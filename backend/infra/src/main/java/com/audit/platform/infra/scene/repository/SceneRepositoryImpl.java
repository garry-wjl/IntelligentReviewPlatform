package com.audit.platform.infra.scene.repository;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.scene.Scene;
import com.audit.platform.domain.scene.repository.SceneRepository;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
import com.audit.platform.infra.scene.entity.SceneEntity;
import com.audit.platform.infra.scene.mapper.SceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class SceneRepositoryImpl implements SceneRepository {

    @Resource
    private SceneMapper sceneMapper;

    @Override
    public void save(Scene aggregate) {
        SceneEntity existed = selectByNum(aggregate.getNum());
        SceneEntity entity = toEntity(aggregate);
        if (existed != null) {
            entity.setId(existed.getId());
            this.sceneMapper.updateById(entity);
        } else {
            this.sceneMapper.insert(entity);
        }
    }

    @Override
    public Scene findByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return null;
        }
        SceneEntity entity = selectByNum(num);
        if (entity == null) {
            return null;
        }
        Scene domain = new Scene();
        domain.setId(entity.getId());
        domain.setNum(entity.getNum());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setExtraParamsJson(entity.getExtraParamsJson());
        domain.setEnabled(entity.getEnabled());
        domain.setCreateId(entity.getCreateId());
        domain.setUpdateId(entity.getUpdateId());
        domain.setCreateTime(entity.getCreateTime());
        domain.setUpdateTime(entity.getUpdateTime());
        domain.setSceneRepository(this);
        return domain;
    }

    @Override
    public void deleteByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return;
        }
        this.sceneMapper.delete(new LambdaQueryWrapper<SceneEntity>().eq(SceneEntity::getNum, num));
    }

    private SceneEntity selectByNum(String num) {
        return this.sceneMapper.selectOne(new LambdaQueryWrapper<SceneEntity>().eq(SceneEntity::getNum, num));
    }

    private SceneEntity toEntity(Scene aggregate) {
        SceneEntity entity = new SceneEntity();
        entity.setNum(aggregate.getNum());
        entity.setName(aggregate.getName());
        entity.setDescription(aggregate.getDescription());
        entity.setExtraParamsJson(aggregate.getExtraParamsJson());
        entity.setEnabled(aggregate.getEnabled());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        return entity;
    }
}
