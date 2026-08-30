package com.audit.platform.infra.evaluation.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.evaluation.Evaluation;
import com.audit.platform.domain.evaluation.repository.EvaluationRepository;
import com.audit.platform.domain.evaluation.valueobject.AnnotationVO;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.RuleResultVO;
import com.audit.platform.domain.evaluation.valueobject.TimelineVO;
import com.audit.platform.infra.common.constant.DeleteFlagConstant;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Repository
public class EvaluationRepositoryImpl implements EvaluationRepository {

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

    @Override
    public void save(Evaluation aggregate) {
        EvaluationEntity existed = selectByNum(aggregate.getNum());
        EvaluationEntity entity = toEntity(aggregate);
        if (existed != null) {
            entity.setId(existed.getId());
            this.evaluationMapper.updateById(entity);
        } else {
            this.evaluationMapper.insert(entity);
        }
        saveAttachments(aggregate);
        saveResults(aggregate);
        saveAnnotations(aggregate);
        saveTimeline(aggregate);
    }

    @Override
    public Evaluation findByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return null;
        }
        EvaluationEntity entity = selectByNum(num);
        if (entity == null) {
            return null;
        }
        Evaluation domain = new Evaluation();
        domain.setId(entity.getId());
        domain.setNum(entity.getNum());
        domain.setBizId(entity.getBizId());
        domain.setTrial(entity.getTrial());
        domain.setStatus(entity.getStatus());
        domain.setAuditorNum(entity.getAuditorNum());
        domain.setAuditorKind(entity.getAuditorKind());
        domain.setAgentName(entity.getAgentName());
        domain.setRuleSetNum(entity.getRuleSetNum());
        domain.setRuleSetVersionNum(entity.getRuleSetVersionNum());
        domain.setRuleSetVersionNo(entity.getRuleSetVersionNo());
        domain.setRuleSetSource(entity.getRuleSetSource());
        domain.setClassifyConfidence(entity.getClassifyConfidence());
        domain.setClassifyReason(entity.getClassifyReason());
        domain.setScoreMode(entity.getScoreMode());
        domain.setOverallPassScore(entity.getOverallPassScore());
        domain.setTotalScore(entity.getTotalScore());
        domain.setPassed(entity.getPassed());
        domain.setComplete(entity.getComplete());
        domain.setFailReason(entity.getFailReason());
        domain.setCredentialNum(entity.getCredentialNum());
        domain.setCallbackUrl(entity.getCallbackUrl());
        domain.setInputText(entity.getInputText());
        domain.setExtraParamsJson(entity.getExtraParamsJson());
        domain.setCreateId(entity.getCreateId());
        domain.setUpdateId(entity.getUpdateId());
        domain.setCreateTime(entity.getCreateTime());
        domain.setUpdateTime(entity.getUpdateTime());
        domain.setAttachments(loadAttachments(entity.getNum()));
        domain.setResults(loadResults(entity.getNum()));
        domain.setAnnotations(loadAnnotations(entity.getNum()));
        domain.setTimeline(loadTimeline(entity.getNum()));
        domain.setEvaluationRepository(this);
        return domain;
    }

    @Override
    public void deleteByNum(String num) {
        if (StrUtil.isBlank(num)) {
            return;
        }
        this.evaluationMapper.delete(new LambdaQueryWrapper<EvaluationEntity>().eq(EvaluationEntity::getNum, num));
        this.evaluationAttachmentMapper.delete(new LambdaQueryWrapper<EvaluationAttachmentEntity>()
                .eq(EvaluationAttachmentEntity::getEvaluationNum, num));
        this.evaluationRuleResultMapper.delete(new LambdaQueryWrapper<EvaluationRuleResultEntity>()
                .eq(EvaluationRuleResultEntity::getEvaluationNum, num));
        this.evaluationAnnotationMapper.delete(new LambdaQueryWrapper<EvaluationAnnotationEntity>()
                .eq(EvaluationAnnotationEntity::getEvaluationNum, num));
        this.evaluationTimelineMapper.delete(new LambdaQueryWrapper<EvaluationTimelineEntity>()
                .eq(EvaluationTimelineEntity::getEvaluationNum, num));
    }

    private void saveAttachments(Evaluation aggregate) {
        this.evaluationAttachmentMapper.delete(new LambdaQueryWrapper<EvaluationAttachmentEntity>()
                .eq(EvaluationAttachmentEntity::getEvaluationNum, aggregate.getNum()));
        if (CollUtil.isEmpty(aggregate.getAttachments())) {
            return;
        }
        for (AttachmentVO vo : aggregate.getAttachments()) {
            EvaluationAttachmentEntity entity = toAttachmentEntity(aggregate, vo);
            upsertChild(this.evaluationAttachmentMapper::selectRawByNum, this.evaluationAttachmentMapper::restoreById,
                    raw -> {
                        entity.setId(raw.getId());
                        entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), aggregate.getCreateId()));
                        entity.setCreateTime(raw.getCreateTime() == null ? aggregate.getCreateTime() : raw.getCreateTime());
                        this.evaluationAttachmentMapper.updateById(entity);
                    }, () -> this.evaluationAttachmentMapper.insert(entity), vo.getNum());
        }
    }

    private void saveResults(Evaluation aggregate) {
        this.evaluationRuleResultMapper.delete(new LambdaQueryWrapper<EvaluationRuleResultEntity>()
                .eq(EvaluationRuleResultEntity::getEvaluationNum, aggregate.getNum()));
        if (CollUtil.isEmpty(aggregate.getResults())) {
            return;
        }
        for (RuleResultVO vo : aggregate.getResults()) {
            EvaluationRuleResultEntity entity = toResultEntity(aggregate, vo);
            upsertChild(this.evaluationRuleResultMapper::selectRawByNum, this.evaluationRuleResultMapper::restoreById,
                    raw -> {
                        entity.setId(raw.getId());
                        entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), aggregate.getCreateId()));
                        entity.setCreateTime(raw.getCreateTime() == null ? aggregate.getCreateTime() : raw.getCreateTime());
                        this.evaluationRuleResultMapper.updateById(entity);
                    }, () -> this.evaluationRuleResultMapper.insert(entity), vo.getNum());
        }
    }

    private void saveAnnotations(Evaluation aggregate) {
        this.evaluationAnnotationMapper.delete(new LambdaQueryWrapper<EvaluationAnnotationEntity>()
                .eq(EvaluationAnnotationEntity::getEvaluationNum, aggregate.getNum()));
        if (CollUtil.isEmpty(aggregate.getAnnotations())) {
            return;
        }
        for (AnnotationVO vo : aggregate.getAnnotations()) {
            EvaluationAnnotationEntity entity = toAnnotationEntity(aggregate, vo);
            upsertChild(this.evaluationAnnotationMapper::selectRawByNum, this.evaluationAnnotationMapper::restoreById,
                    raw -> {
                        entity.setId(raw.getId());
                        entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), aggregate.getCreateId()));
                        entity.setCreateTime(raw.getCreateTime() == null ? aggregate.getCreateTime() : raw.getCreateTime());
                        this.evaluationAnnotationMapper.updateById(entity);
                    }, () -> this.evaluationAnnotationMapper.insert(entity), vo.getNum());
        }
    }

    private void saveTimeline(Evaluation aggregate) {
        this.evaluationTimelineMapper.delete(new LambdaQueryWrapper<EvaluationTimelineEntity>()
                .eq(EvaluationTimelineEntity::getEvaluationNum, aggregate.getNum()));
        if (CollUtil.isEmpty(aggregate.getTimeline())) {
            return;
        }
        for (TimelineVO vo : aggregate.getTimeline()) {
            EvaluationTimelineEntity entity = toTimelineEntity(aggregate, vo);
            upsertChild(this.evaluationTimelineMapper::selectRawByNum, this.evaluationTimelineMapper::restoreById,
                    raw -> {
                        entity.setId(raw.getId());
                        entity.setCreateId(StrUtil.blankToDefault(raw.getCreateId(), aggregate.getCreateId()));
                        entity.setCreateTime(raw.getCreateTime() == null ? aggregate.getCreateTime() : raw.getCreateTime());
                        this.evaluationTimelineMapper.updateById(entity);
                    }, () -> this.evaluationTimelineMapper.insert(entity), vo.getNum());
        }
    }

    private <T> void upsertChild(Function<String, T> selectRaw, Function<Long, Integer> restore,
                                 java.util.function.Consumer<T> update, Runnable insert, String num) {
        T raw = StrUtil.isBlank(num) ? null : selectRaw.apply(num);
        if (raw != null) {
            Long id = extractId(raw);
            restore.apply(id);
            update.accept(raw);
        } else {
            insert.run();
        }
    }

    private Long extractId(Object raw) {
        if (raw instanceof EvaluationAttachmentEntity e) {
            return e.getId();
        }
        if (raw instanceof EvaluationRuleResultEntity e) {
            return e.getId();
        }
        if (raw instanceof EvaluationAnnotationEntity e) {
            return e.getId();
        }
        if (raw instanceof EvaluationTimelineEntity e) {
            return e.getId();
        }
        return null;
    }

    private List<AttachmentVO> loadAttachments(String evaluationNum) {
        List<AttachmentVO> list = new ArrayList<>();
        for (EvaluationAttachmentEntity row : this.evaluationAttachmentMapper.selectList(
                new LambdaQueryWrapper<EvaluationAttachmentEntity>()
                        .eq(EvaluationAttachmentEntity::getEvaluationNum, evaluationNum)
                        .orderByAsc(EvaluationAttachmentEntity::getSortNo))) {
            AttachmentVO vo = new AttachmentVO();
            vo.setId(row.getId());
            vo.setNum(row.getNum());
            vo.setObjectKey(row.getObjectKey());
            vo.setFileName(row.getFileName());
            vo.setMime(row.getMime());
            vo.setRole(row.getRole());
            vo.setSortNo(row.getSortNo());
            vo.setParseFailed(row.getParseFailed());
            vo.setExcerpt(row.getExcerpt());
            list.add(vo);
        }
        return list;
    }

    private List<RuleResultVO> loadResults(String evaluationNum) {
        List<RuleResultVO> list = new ArrayList<>();
        for (EvaluationRuleResultEntity row : this.evaluationRuleResultMapper.selectList(
                new LambdaQueryWrapper<EvaluationRuleResultEntity>()
                        .eq(EvaluationRuleResultEntity::getEvaluationNum, evaluationNum))) {
            RuleResultVO vo = new RuleResultVO();
            vo.setId(row.getId());
            vo.setNum(row.getNum());
            vo.setRuleNum(row.getRuleNum());
            vo.setRuleName(row.getRuleName());
            vo.setStandard(row.getStandard());
            vo.setMinScore(row.getMinScore());
            vo.setMaxScore(row.getMaxScore());
            vo.setPassScore(row.getPassScore());
            vo.setWeight(row.getWeight());
            vo.setVeto(row.getVeto());
            vo.setMachineScore(row.getMachineScore());
            vo.setMachineRationale(row.getMachineRationale());
            vo.setHumanScore(row.getHumanScore());
            vo.setHumanReason(row.getHumanReason());
            vo.setFailed(row.getFailed());
            vo.setFailReason(row.getFailReason());
            vo.setEvidenceJson(row.getEvidenceJson());
            list.add(vo);
        }
        return list;
    }

    private List<AnnotationVO> loadAnnotations(String evaluationNum) {
        List<AnnotationVO> list = new ArrayList<>();
        for (EvaluationAnnotationEntity row : this.evaluationAnnotationMapper.selectList(
                new LambdaQueryWrapper<EvaluationAnnotationEntity>()
                        .eq(EvaluationAnnotationEntity::getEvaluationNum, evaluationNum))) {
            AnnotationVO vo = new AnnotationVO();
            vo.setId(row.getId());
            vo.setNum(row.getNum());
            vo.setTarget(row.getTarget());
            vo.setRuleNum(row.getRuleNum());
            vo.setFileNum(row.getFileNum());
            vo.setLocation(row.getLocation());
            vo.setContent(row.getContent());
            list.add(vo);
        }
        return list;
    }

    private List<TimelineVO> loadTimeline(String evaluationNum) {
        List<TimelineVO> list = new ArrayList<>();
        for (EvaluationTimelineEntity row : this.evaluationTimelineMapper.selectList(
                new LambdaQueryWrapper<EvaluationTimelineEntity>()
                        .eq(EvaluationTimelineEntity::getEvaluationNum, evaluationNum)
                        .orderByAsc(EvaluationTimelineEntity::getId))) {
            TimelineVO vo = new TimelineVO();
            vo.setId(row.getId());
            vo.setNum(row.getNum());
            vo.setActor(row.getActor());
            vo.setTitle(row.getTitle());
            vo.setDetail(row.getDetail());
            list.add(vo);
        }
        return list;
    }

    private EvaluationEntity selectByNum(String num) {
        return this.evaluationMapper.selectOne(new LambdaQueryWrapper<EvaluationEntity>().eq(EvaluationEntity::getNum, num));
    }

    private EvaluationEntity toEntity(Evaluation aggregate) {
        EvaluationEntity entity = new EvaluationEntity();
        entity.setNum(aggregate.getNum());
        entity.setBizId(aggregate.getBizId());
        entity.setTrial(aggregate.getTrial());
        entity.setStatus(aggregate.getStatus());
        entity.setAuditorNum(aggregate.getAuditorNum());
        entity.setAuditorKind(aggregate.getAuditorKind());
        entity.setAgentName(aggregate.getAgentName());
        entity.setRuleSetNum(aggregate.getRuleSetNum());
        entity.setRuleSetVersionNum(aggregate.getRuleSetVersionNum());
        entity.setRuleSetVersionNo(aggregate.getRuleSetVersionNo());
        entity.setRuleSetSource(aggregate.getRuleSetSource());
        entity.setClassifyConfidence(aggregate.getClassifyConfidence());
        entity.setClassifyReason(aggregate.getClassifyReason());
        entity.setScoreMode(aggregate.getScoreMode());
        entity.setOverallPassScore(aggregate.getOverallPassScore());
        entity.setTotalScore(aggregate.getTotalScore());
        entity.setPassed(aggregate.getPassed());
        entity.setComplete(aggregate.getComplete());
        entity.setFailReason(aggregate.getFailReason());
        entity.setCredentialNum(aggregate.getCredentialNum());
        entity.setCallbackUrl(aggregate.getCallbackUrl());
        entity.setInputText(aggregate.getInputText());
        entity.setExtraParamsJson(aggregate.getExtraParamsJson());
        entity.setCreateId(aggregate.getCreateId());
        entity.setUpdateId(aggregate.getUpdateId());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        entity.setIsDeleted(DeleteFlagConstant.NOT_DELETED);
        return entity;
    }

    private EvaluationAttachmentEntity toAttachmentEntity(Evaluation aggregate, AttachmentVO vo) {
        EvaluationAttachmentEntity entity = new EvaluationAttachmentEntity();
        entity.setNum(vo.getNum());
        entity.setEvaluationNum(aggregate.getNum());
        entity.setObjectKey(StrUtil.blankToDefault(vo.getObjectKey(), ""));
        entity.setFileName(vo.getFileName());
        entity.setMime(vo.getMime() == null ? "" : vo.getMime());
        entity.setRole(vo.getRole());
        entity.setSortNo(vo.getSortNo());
        entity.setParseFailed(Boolean.TRUE.equals(vo.getParseFailed()));
        entity.setExcerpt(vo.getExcerpt() == null ? "" : vo.getExcerpt());
        fillAudit(entity::setCreateId, entity::setUpdateId, entity::setCreateTime, entity::setUpdateTime,
                entity::setIsDeleted, aggregate);
        return entity;
    }

    private EvaluationRuleResultEntity toResultEntity(Evaluation aggregate, RuleResultVO vo) {
        EvaluationRuleResultEntity entity = new EvaluationRuleResultEntity();
        entity.setNum(vo.getNum());
        entity.setEvaluationNum(aggregate.getNum());
        entity.setRuleNum(vo.getRuleNum());
        entity.setRuleName(vo.getRuleName());
        entity.setStandard(vo.getStandard());
        entity.setMinScore(vo.getMinScore());
        entity.setMaxScore(vo.getMaxScore());
        entity.setPassScore(vo.getPassScore());
        entity.setWeight(vo.getWeight());
        entity.setVeto(vo.getVeto());
        entity.setMachineScore(vo.getMachineScore());
        entity.setMachineRationale(vo.getMachineRationale());
        entity.setHumanScore(vo.getHumanScore());
        entity.setHumanReason(vo.getHumanReason());
        entity.setFailed(Boolean.TRUE.equals(vo.getFailed()));
        entity.setFailReason(vo.getFailReason());
        entity.setEvidenceJson(vo.getEvidenceJson());
        fillAudit(entity::setCreateId, entity::setUpdateId, entity::setCreateTime, entity::setUpdateTime,
                entity::setIsDeleted, aggregate);
        return entity;
    }

    private EvaluationAnnotationEntity toAnnotationEntity(Evaluation aggregate, AnnotationVO vo) {
        EvaluationAnnotationEntity entity = new EvaluationAnnotationEntity();
        entity.setNum(vo.getNum());
        entity.setEvaluationNum(aggregate.getNum());
        entity.setTarget(vo.getTarget());
        entity.setRuleNum(vo.getRuleNum());
        entity.setFileNum(vo.getFileNum());
        entity.setLocation(vo.getLocation());
        entity.setContent(vo.getContent());
        fillAudit(entity::setCreateId, entity::setUpdateId, entity::setCreateTime, entity::setUpdateTime,
                entity::setIsDeleted, aggregate);
        return entity;
    }

    private EvaluationTimelineEntity toTimelineEntity(Evaluation aggregate, TimelineVO vo) {
        EvaluationTimelineEntity entity = new EvaluationTimelineEntity();
        entity.setNum(vo.getNum());
        entity.setEvaluationNum(aggregate.getNum());
        entity.setActor(vo.getActor());
        entity.setTitle(vo.getTitle());
        entity.setDetail(vo.getDetail());
        fillAudit(entity::setCreateId, entity::setUpdateId, entity::setCreateTime, entity::setUpdateTime,
                entity::setIsDeleted, aggregate);
        return entity;
    }

    private void fillAudit(java.util.function.Consumer<String> createId,
                           java.util.function.Consumer<String> updateId,
                           java.util.function.Consumer<java.time.LocalDateTime> createTime,
                           java.util.function.Consumer<java.time.LocalDateTime> updateTime,
                           java.util.function.Consumer<Integer> isDeleted,
                           Evaluation aggregate) {
        createId.accept(aggregate.getCreateId());
        updateId.accept(aggregate.getUpdateId());
        createTime.accept(aggregate.getCreateTime());
        updateTime.accept(aggregate.getUpdateTime());
        isDeleted.accept(DeleteFlagConstant.NOT_DELETED);
    }
}
