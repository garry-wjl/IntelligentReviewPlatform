package com.audit.platform.application.evaluation;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.audit.platform.application.access.AccessQueryService;
import com.audit.platform.application.auditor.AuditorQueryService;
import com.audit.platform.application.ruleset.RuleSetQueryService;
import com.audit.platform.client.access.dto.IntegrationDTO;
import com.audit.platform.client.auditor.dto.AuditorDTO;
import com.audit.platform.client.auditor.dto.AuditorNumParamDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.evaluation.dto.AttachmentParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationAnnotateParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationAssignParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationBizQueryParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationCreateParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationCreatedDTO;
import com.audit.platform.client.evaluation.dto.EvaluationDetailDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationScoreParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetNumParamDTO;
import com.audit.platform.domain.evaluation.Evaluation;
import com.audit.platform.domain.evaluation.factory.EvaluationFactory;
import com.audit.platform.domain.evaluation.valueobject.AnnotationVO;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.common.constant.LockKeyConstant;
import com.audit.platform.infra.common.lock.RedisLockHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationCommandService {
    @Resource
    private EvaluationFactory evaluationFactory;
    @Resource
    private EvaluationQueryService evaluationQueryService;
    @Resource
    private AuditorQueryService auditorQueryService;
    @Resource
    private RuleSetQueryService ruleSetQueryService;
    @Resource
    private AccessQueryService accessQueryService;
    @Resource
    private RedisLockHelper redisLockHelper;

    @Transactional(rollbackFor = Exception.class)
    public EvaluationCreatedDTO create(EvaluationCreateParamDTO param) {
        String bizKey = StrUtil.blankToDefault(param.getCredentialNum(), "admin") + ":" + param.getBizId();
        return redisLockHelper.execute(LockKeyConstant.EVALUATION_BIZ + bizKey, () -> {
            if (!Boolean.TRUE.equals(param.getTrial())) {
                EvaluationBizQueryParamDTO bizQuery = new EvaluationBizQueryParamDTO();
                bizQuery.setBizId(param.getBizId());
                bizQuery.setCredentialNum(param.getCredentialNum());
                bizQuery.setTrial(Boolean.FALSE);
                EvaluationDetailDTO existed = evaluationQueryService.findByBizId(bizQuery);
                if (existed != null) {
                    return EvaluationCreatedDTO.builder().num(existed.getNum()).idempotent(Boolean.TRUE).build();
                }
            }
            AuditorDTO auditor = null;
            if (StrUtil.isNotBlank(param.getAuditorNum())) {
                AuditorNumParamDTO auditorParam = new AuditorNumParamDTO();
                auditorParam.setNum(param.getAuditorNum());
                auditor = auditorQueryService.detail(auditorParam);
                if (auditor == null || !Boolean.TRUE.equals(auditor.getEnabled())) {
                    throw new BusinessException("审核器不可用");
                }
                if ("AUD-SYS".equals(auditor.getNum())) {
                    throw new BusinessException("系统占位审核器不可用于任务");
                }
            }
            if (StrUtil.isNotBlank(param.getRuleSetNum()) && !Boolean.TRUE.equals(param.getTrial())) {
                RuleSetNumParamDTO ruleSetParam = new RuleSetNumParamDTO();
                ruleSetParam.setNum(param.getRuleSetNum());
                ruleSetQueryService.requirePublished(ruleSetParam);
            }
            List<AttachmentVO> attachments = new ArrayList<>();
            for (AttachmentParamDTO item : param.getAttachments()) {
                attachments.add(AttachmentVO.builder()
                        .objectKey(item.getObjectKey())
                        .fileUrl(item.getFileUrl())
                        .fileName(item.getFileName())
                        .mime(item.getMime())
                        .role(item.getRole())
                        .build());
            }
            Evaluation evaluation = evaluationFactory.create(param.getBizId(), param.getAuditorNum(),
                    param.getRuleSetNum(), param.getTrial(), attachments);
            evaluation.setInputText(param.getInputText());
            evaluation.setExtraParamsJson(param.getExtraParams() == null ? "{}"
                    : JSONUtil.toJsonStr(param.getExtraParams()));
            evaluation.bindAuditorSnapshot(
                    auditor == null ? "PER_RULE" : auditor.getKind(),
                    auditor == null ? null : auditor.getAgentName());
            evaluation.bindCredential(param.getCredentialNum(), param.getCallbackUrl());
            evaluation.bindDraftVersion(param.getRuleSetVersionNum());
            evaluation.save(param.getOperatorId());
            return EvaluationCreatedDTO.builder().num(evaluation.getNum()).idempotent(Boolean.FALSE).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void runPipeline(EvaluationNumParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + "pipeline:" + param.getNum(), () -> {
            Evaluation evaluation = evaluationFactory.createByNum(param.getNum());
            if ("RECEIVED".equals(evaluation.getStatus())) {
                evaluation.startParse(param.getOperatorId());
            }
            if ("PARSING".equals(evaluation.getStatus())) {
                EmptyParamDTO empty = new EmptyParamDTO();
                IntegrationDTO integration = accessQueryService.getIntegration(empty);
                evaluation.matchRuleSet(param.getOperatorId(), integration.getClassifyThreshold());
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void patchScore(EvaluationScoreParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + param.getNum(), () -> {
            Evaluation evaluation = evaluationFactory.createByNum(param.getNum());
            evaluation.patchScore(param.getRuleNum(), param.getScore(), param.getReason(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void annotate(EvaluationAnnotateParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + param.getNum(), () -> {
            Evaluation evaluation = evaluationFactory.createByNum(param.getNum());
            evaluation.addAnnotation(AnnotationVO.builder()
                    .target(param.getTarget())
                    .ruleNum(param.getRuleNum())
                    .fileNum(param.getFileNum())
                    .location(param.getLocation())
                    .content(param.getContent())
                    .build(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalizeScore(EvaluationNumParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + param.getNum(), () -> {
            Evaluation evaluation = evaluationFactory.createByNum(param.getNum());
            evaluation.finalizeScore(param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void recoverStuck(EmptyParamDTO param) {
        for (String num : evaluationQueryService.listStuckNums(param)) {
            EvaluationNumParamDTO numParam = new EvaluationNumParamDTO();
            numParam.setNum(num);
            numParam.setOperatorId(param.getOperatorId());
            runPipeline(numParam);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRuleSet(EvaluationAssignParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.EVALUATION + param.getNum(), () -> {
            Evaluation evaluation = evaluationFactory.createByNum(param.getNum());
            evaluation.assignRuleSet(param.getRuleSetNum(), param.getOperatorId());
        });
    }
}
