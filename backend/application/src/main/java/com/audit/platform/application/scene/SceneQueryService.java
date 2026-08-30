package com.audit.platform.application.scene;

import cn.hutool.core.bean.BeanUtil;
import com.audit.platform.application.common.PageQueryHelper;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.scene.dto.SceneDTO;
import com.audit.platform.client.scene.dto.SceneNumParamDTO;
import com.audit.platform.client.scene.dto.ScenePageParamDTO;
import com.audit.platform.client.scene.dto.SceneParamDTO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.domain.scene.valueobject.SceneParamVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.scene.entity.SceneEntity;
import com.audit.platform.infra.scene.mapper.SceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SceneQueryService {
    @Resource
    private SceneMapper sceneMapper;

    public PageDTO<SceneDTO> page(ScenePageParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<SceneEntity> wrapper = new LambdaQueryWrapper<>();
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), param.getName(), param.getKeyword(),
                SceneEntity::getNum, SceneEntity::getName);
        wrapper.eq(param.getEnabled() != null, SceneEntity::getEnabled, param.getEnabled());
        wrapper.orderByDesc(SceneEntity::getUpdateTime);
        Page<SceneEntity> page = sceneMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<SceneDTO> list = new ArrayList<>();
        for (SceneEntity entity : page.getRecords()) {
            list.add(toDto(entity));
        }
        return PageDTO.<SceneDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public SceneDTO detail(SceneNumParamDTO param) {
        SceneEntity entity = sceneMapper.selectOne(new LambdaQueryWrapper<SceneEntity>()
                .eq(SceneEntity::getNum, param.getNum()));
        if (entity == null) {
            throw new BusinessException("场景不存在");
        }
        return toDto(entity);
    }

    public List<SceneDTO> listEnabled() {
        List<SceneEntity> rows = sceneMapper.selectList(new LambdaQueryWrapper<SceneEntity>()
                .eq(SceneEntity::getEnabled, Boolean.TRUE)
                .orderByDesc(SceneEntity::getUpdateTime));
        List<SceneDTO> list = new ArrayList<>();
        for (SceneEntity row : rows) {
            list.add(toDto(row));
        }
        return list;
    }

    private SceneDTO toDto(SceneEntity entity) {
        SceneDTO dto = BeanUtil.copyProperties(entity, SceneDTO.class);
        List<SceneParamDTO> extras = new ArrayList<>();
        for (SceneParamVO extra : SceneParamCatalog.parseExtra(entity.getExtraParamsJson())) {
            extras.add(toParam(extra));
        }
        List<SceneParamDTO> params = new ArrayList<>();
        for (SceneParamVO param : SceneParamCatalog.resolve(entity.getExtraParamsJson())) {
            params.add(toParam(param));
        }
        dto.setExtraParams(extras);
        dto.setParams(params);
        return dto;
    }

    private SceneParamDTO toParam(SceneParamVO vo) {
        return SceneParamDTO.builder()
                .key(vo.getKey())
                .label(vo.getLabel())
                .type(vo.getType())
                .builtin(vo.getBuiltin())
                .build();
    }
}
