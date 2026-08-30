package com.audit.platform.application.evaluation;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.application.common.PageQueryHelper;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.evaluation.dto.AnnotationDTO;
import com.audit.platform.client.evaluation.dto.AttachmentDTO;
import com.audit.platform.client.evaluation.dto.AttachmentUrlDTO;
import com.audit.platform.client.evaluation.dto.AttachmentUrlParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationBizQueryParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationDetailDTO;
import com.audit.platform.client.evaluation.dto.EvaluationListDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationPageParamDTO;
import com.audit.platform.client.evaluation.dto.RuleResultDTO;
import com.audit.platform.client.evaluation.dto.TimelineDTO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.common.client.ObjectStorageClient;
import com.audit.platform.infra.evaluation.entity.EvaluationAnnotationEntity;
import com.audit.platform.infra.evaluation.entity.EvaluationAttachmentEntity;
import com.audit.platform.infra.evaluation.entity.EvaluationEntity;
import com.audit.platform.infra.evaluation.entity.EvaluationRuleResultEntity;
import com.audit.platform.infra.evaluation.entity.EvaluationTimelineEntity;
import com.audit.platform.infra.evaluation.mapper.EvaluationAnnotationMapper;
import com.audit.platform.infra.evaluation.mapper.EvaluationAttachmentMapper;
import com.audit.platform.infra.evaluation.mapper.EvaluationMapper;
import com.audit.platform.infra.evaluation.mapper.EvaluationRuleResultMapper;
import com.audit.platform.infra.evaluation.mapper.EvaluationTimelineMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationQueryService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private EvaluationMapper evaluationMapper;
    @Resource
    private EvaluationAttachmentMapper evaluationAttachmentMapper;
    @Resource
    private EvaluationRuleResultMapper evaluationRuleResultMapper;
    @Resource
    private EvaluationAnnotationMapper evaluationAnnotationMapper;
    @Resource
    private EvaluationTimelineMapper evaluationTimelineMapper;
    @Resource
    private ObjectStorageClient objectStorageClient;

    public PageDTO<EvaluationListDTO> page(EvaluationPageParamDTO param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 20 : param.getPageSize();
        LambdaQueryWrapper<EvaluationEntity> wrapper = new LambdaQueryWrapper<>();
        String name = StrUtil.blankToDefault(param.getName(), param.getBizId());
        PageQueryHelper.likeNumAndName(wrapper, param.getNum(), name, param.getKeyword(),
                EvaluationEntity::getNum, EvaluationEntity::getBizId);
        wrapper.eq(StrUtil.isNotBlank(param.getRuleSetNum()), EvaluationEntity::getRuleSetNum, param.getRuleSetNum());
        wrapper.eq(StrUtil.isNotBlank(param.getAuditorNum()), EvaluationEntity::getAuditorNum, param.getAuditorNum());
        wrapper.eq(StrUtil.isNotBlank(param.getStatus()), EvaluationEntity::getStatus, param.getStatus());
        wrapper.eq(param.getIsTrial() != null, EvaluationEntity::getTrial, param.getIsTrial());
        if (StrUtil.isNotBlank(param.getCreateTimeFrom())) {
            wrapper.ge(EvaluationEntity::getCreateTime, LocalDateTime.parse(param.getCreateTimeFrom(), TIME));
        }
        if (StrUtil.isNotBlank(param.getCreateTimeTo())) {
            wrapper.le(EvaluationEntity::getCreateTime, LocalDateTime.parse(param.getCreateTimeTo(), TIME));
        }
        wrapper.orderByDesc(EvaluationEntity::getCreateTime);
        Page<EvaluationEntity> page = evaluationMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<EvaluationListDTO> list = new ArrayList<>();
        for (EvaluationEntity entity : page.getRecords()) {
            list.add(BeanUtil.copyProperties(entity, EvaluationListDTO.class));
        }
        return PageDTO.<EvaluationListDTO>builder().total(page.getTotal()).pageNo(pageNo).pageSize(pageSize).list(list).build();
    }

    public EvaluationDetailDTO detail(EvaluationNumParamDTO param) {
        EvaluationEntity entity = evaluationMapper.selectOne(new LambdaQueryWrapper<EvaluationEntity>()
                .eq(EvaluationEntity::getNum, param.getNum()));
        if (entity == null) {
            throw new BusinessException("审核任务不存在");
        }
        return toDetail(entity);
    }

    public EvaluationDetailDTO findByBizId(EvaluationBizQueryParamDTO param) {
        LambdaQueryWrapper<EvaluationEntity> wrapper = new LambdaQueryWrapper<EvaluationEntity>()
                .eq(EvaluationEntity::getBizId, param.getBizId())
                .eq(EvaluationEntity::getTrial, Boolean.TRUE.equals(param.getTrial()));
        if (StrUtil.isNotBlank(param.getCredentialNum())) {
            wrapper.eq(EvaluationEntity::getCredentialNum, param.getCredentialNum());
        }
        wrapper.last("limit 1");
        EvaluationEntity entity = evaluationMapper.selectOne(wrapper);
        return entity == null ? null : toDetail(entity);
    }

    public AttachmentUrlDTO attachmentUrl(AttachmentUrlParamDTO param) {
        EvaluationAttachmentEntity attachment = evaluationAttachmentMapper.selectOne(
                new LambdaQueryWrapper<EvaluationAttachmentEntity>()
                        .eq(EvaluationAttachmentEntity::getEvaluationNum, param.getEvaluationNum())
                        .eq(EvaluationAttachmentEntity::getNum, param.getFileNum()));
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        return AttachmentUrlDTO.builder()
                .url(objectStorageClient.presignGet(attachment.getObjectKey()))
                .fileName(attachment.getFileName())
                .build();
    }

    private EvaluationDetailDTO toDetail(EvaluationEntity entity) {
        EvaluationDetailDTO dto = BeanUtil.copyProperties(entity, EvaluationDetailDTO.class);
        List<AttachmentDTO> attachments = new ArrayList<>();
        for (EvaluationAttachmentEntity row : evaluationAttachmentMapper.selectList(
                new LambdaQueryWrapper<EvaluationAttachmentEntity>()
                        .eq(EvaluationAttachmentEntity::getEvaluationNum, entity.getNum())
                        .orderByAsc(EvaluationAttachmentEntity::getSortNo))) {
            attachments.add(BeanUtil.copyProperties(row, AttachmentDTO.class));
        }
        dto.setAttachments(attachments);
        List<RuleResultDTO> results = new ArrayList<>();
        for (EvaluationRuleResultEntity row : evaluationRuleResultMapper.selectList(
                new LambdaQueryWrapper<EvaluationRuleResultEntity>()
                        .eq(EvaluationRuleResultEntity::getEvaluationNum, entity.getNum()))) {
            RuleResultDTO item = BeanUtil.copyProperties(row, RuleResultDTO.class);
            item.setDisplayScore(item.getHumanScore() != null ? item.getHumanScore() : item.getMachineScore());
            results.add(item);
        }
        dto.setResults(results);
        List<AnnotationDTO> annotations = new ArrayList<>();
        for (EvaluationAnnotationEntity row : evaluationAnnotationMapper.selectList(
                new LambdaQueryWrapper<EvaluationAnnotationEntity>()
                        .eq(EvaluationAnnotationEntity::getEvaluationNum, entity.getNum()))) {
            annotations.add(BeanUtil.copyProperties(row, AnnotationDTO.class));
        }
        dto.setAnnotations(annotations);
        List<TimelineDTO> timeline = new ArrayList<>();
        for (EvaluationTimelineEntity row : evaluationTimelineMapper.selectList(
                new LambdaQueryWrapper<EvaluationTimelineEntity>()
                        .eq(EvaluationTimelineEntity::getEvaluationNum, entity.getNum())
                        .orderByAsc(EvaluationTimelineEntity::getId))) {
            timeline.add(BeanUtil.copyProperties(row, TimelineDTO.class));
        }
        dto.setTimeline(timeline);
        return dto;
    }

    public List<String> listStuckNums(com.audit.platform.client.common.dto.EmptyParamDTO param) {
        List<EvaluationEntity> rows = evaluationMapper.selectList(new LambdaQueryWrapper<EvaluationEntity>()
                .in(EvaluationEntity::getStatus, List.of("RECEIVED", "PARSING", "CLASSIFYING", "SCORING"))
                .le(EvaluationEntity::getUpdateTime, LocalDateTime.now().minusMinutes(1))
                .last("limit 20"));
        List<String> nums = new ArrayList<>();
        for (EvaluationEntity row : rows) {
            nums.add(row.getNum());
        }
        return nums;
    }
}
