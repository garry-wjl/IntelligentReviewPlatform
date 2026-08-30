package com.audit.platform.application.auditor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.application.common.PageQueryHelper;
import com.audit.platform.client.auditor.dto.AgentOptionDTO;
import com.audit.platform.client.auditor.dto.AuditorDTO;
import com.audit.platform.client.auditor.dto.AuditorNumParamDTO;
import com.audit.platform.client.auditor.dto.AuditorPageParamDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.auditor.entity.AgentCatalogEntity;
import com.audit.platform.infra.auditor.entity.AuditorEntity;
import com.audit.platform.infra.auditor.mapper.AgentCatalogMapper;
import com.audit.platform.infra.auditor.mapper.AuditorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditorQueryService {
    @Resource
    private AuditorMapper auditorMapper;
    @Resource
    private AgentCatalogMapper agentCatalogMapper;

    public PageDTO<AuditorDTO> page(AuditorPageParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<AuditorEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(AuditorEntity::getNum, AuditorCommandService.SYSTEM_AUDITOR_NUM);
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), param.getName(), param.getKeyword(),
                AuditorEntity::getNum, AuditorEntity::getName);
        wrapper.eq(StrUtil.isNotBlank(param.getKind()), AuditorEntity::getKind, param.getKind());
        wrapper.eq(param.getEnabled() != null, AuditorEntity::getEnabled, param.getEnabled());
        wrapper.orderByDesc(AuditorEntity::getUpdateTime);
        Page<AuditorEntity> page = auditorMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<AuditorDTO> list = new ArrayList<>();
        for (AuditorEntity entity : page.getRecords()) {
            list.add(BeanUtil.copyProperties(entity, AuditorDTO.class));
        }
        return PageDTO.<AuditorDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public AuditorDTO detail(AuditorNumParamDTO param) {
        AuditorEntity entity = auditorMapper.selectOne(new LambdaQueryWrapper<AuditorEntity>()
                .eq(AuditorEntity::getNum, param.getNum()));
        if (entity == null) {
            throw new BusinessException("审核器不存在");
        }
        return BeanUtil.copyProperties(entity, AuditorDTO.class);
    }

    public List<AgentOptionDTO> listAgents(EmptyParamDTO param) {
        List<AgentCatalogEntity> rows = agentCatalogMapper.selectList(new LambdaQueryWrapper<AgentCatalogEntity>()
                .orderByAsc(AgentCatalogEntity::getId));
        List<AgentOptionDTO> list = new ArrayList<>();
        for (AgentCatalogEntity row : rows) {
            list.add(AgentOptionDTO.builder()
                    .agentNum(row.getAgentNum())
                    .name(row.getName())
                    .description(row.getDescription())
                    .provider(row.getProvider())
                    .build());
        }
        return list;
    }
}
