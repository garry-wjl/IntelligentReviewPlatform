package com.audit.platform.infra.evaluation.gateway;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.audit.platform.domain.evaluation.gateway.EvaluationGateway;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.ClassifyResultVO;
import com.audit.platform.domain.evaluation.valueobject.RuleItemSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.RuleScoreVO;
import com.audit.platform.domain.evaluation.valueobject.RuleSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.ScoreContextVO;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.auditor.entity.AuditorEntity;
import com.audit.platform.infra.auditor.mapper.AuditorMapper;
import com.audit.platform.infra.common.client.ObjectStorageClient;
import com.audit.platform.infra.common.client.ScoringClient;
import com.audit.platform.infra.common.client.dto.ScoreResultDTO;
import com.audit.platform.infra.common.client.param.ScoreRequestParam;
import com.audit.platform.infra.common.util.NumGenerator;
import com.audit.platform.infra.evaluation.engine.OrdinaryRuleEngine;
import com.audit.platform.infra.ruleset.entity.RuleItemEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetEntity;
import com.audit.platform.infra.ruleset.entity.RuleSetVersionEntity;
import com.audit.platform.infra.ruleset.mapper.RuleItemMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetMapper;
import com.audit.platform.infra.ruleset.mapper.RuleSetVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class EvaluationGatewayImpl implements EvaluationGateway {

    @Resource
    private NumGenerator numGenerator;
    @Resource
    private ObjectStorageClient objectStorageClient;
    @Resource
    private ScoringClient scoringClient;
    @Resource
    private OrdinaryRuleEngine ordinaryRuleEngine;
    @Resource
    private AuditorMapper auditorMapper;
    @Resource
    private RuleSetMapper ruleSetMapper;
    @Resource
    private RuleSetVersionMapper ruleSetVersionMapper;
    @Resource
    private RuleItemMapper ruleItemMapper;

    @Override
    public String generateNum(boolean trial) {
        return trial ? this.numGenerator.next("TRL-", 6) : this.numGenerator.next("EVL-", 6);
    }

    @Override
    public String generateChildNum(String prefix) {
        String normalized = prefix.endsWith("-") ? prefix : prefix + "-";
        return this.numGenerator.next(normalized, 6);
    }

    @Override
    public void ingestRemoteFiles(List<AttachmentVO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }
        for (AttachmentVO attachment : attachments) {
            if (StrUtil.isNotBlank(attachment.getFileUrl()) && StrUtil.isBlank(attachment.getObjectKey())) {
                String objectKey = "eval/ingest/" + IdUtil.fastSimpleUUID() + "-"
                        + StrUtil.blankToDefault(attachment.getFileName(), "file");
                this.objectStorageClient.put(objectKey, attachment.getFileUrl().getBytes(StandardCharsets.UTF_8));
                attachment.setObjectKey(objectKey);
            }
        }
    }

    @Override
    public void parse(List<AttachmentVO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }
        for (AttachmentVO attachment : attachments) {
            try {
                if (StrUtil.isNotBlank(attachment.getObjectKey())) {
                    byte[] data = this.objectStorageClient.get(attachment.getObjectKey());
                    String text = data == null ? "" : new String(data, StandardCharsets.UTF_8);
                    attachment.setExcerpt(StrUtil.maxLength(StrUtil.blankToDefault(text, attachment.getFileName()), 2000));
                } else {
                    attachment.setExcerpt(StrUtil.blankToDefault(attachment.getFileName(), ""));
                }
                attachment.setParseFailed(Boolean.FALSE);
            } catch (Exception e) {
                attachment.setParseFailed(Boolean.TRUE);
                attachment.setExcerpt("");
            }
        }
    }

    @Override
    public ClassifyResultVO classify(List<AttachmentVO> attachments) {
        RuleSetEntity ruleSet = this.ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getEnabled, Boolean.TRUE)
                .isNotNull(RuleSetEntity::getCurrentPublishedVersionNum)
                .ne(RuleSetEntity::getCurrentPublishedVersionNum, "")
                .orderByAsc(RuleSetEntity::getId)
                .last("limit 1"));
        if (ruleSet == null) {
            return ClassifyResultVO.builder()
                    .ruleSetNum(null)
                    .confidence(new BigDecimal("0.2"))
                    .reason("无可用已发布规则集")
                    .build();
        }
        return ClassifyResultVO.builder()
                .ruleSetNum(ruleSet.getNum())
                .confidence(new BigDecimal("0.85"))
                .reason("匹配启用且已发布规则集")
                .build();
    }

    @Override
    public RuleSnapshotVO loadPublishedRules(String ruleSetNum) {
        RuleSetEntity ruleSet = this.ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, ruleSetNum));
        if (ruleSet == null || StrUtil.isBlank(ruleSet.getCurrentPublishedVersionNum())) {
            throw new BusinessException("规则集未发布");
        }
        return toSnapshot(ruleSet.getNum(), loadVersion(ruleSet.getCurrentPublishedVersionNum()));
    }

    @Override
    public RuleSnapshotVO loadDraftRules(String ruleSetVersionNum) {
        RuleSetVersionEntity version = loadVersion(ruleSetVersionNum);
        return toSnapshot(version.getRuleSetNum(), version);
    }

    @Override
    public List<RuleScoreVO> score(RuleSnapshotVO snapshot, ScoreContextVO context, String auditorKind,
                                   String agentNum) {
        List<RuleScoreVO> scores = new ArrayList<>();
        if (snapshot == null || CollUtil.isEmpty(snapshot.getRules())) {
            return scores;
        }
        for (RuleItemSnapshotVO rule : snapshot.getRules()) {
            String kind = StrUtil.blankToDefault(rule.getAuditorKind(), auditorKind);
            String agent = StrUtil.blankToDefault(rule.getAgentName(), agentNum);
            if ("AGENT".equals(rule.getEngineKind())) {
                ScoreRequestParam param = new ScoreRequestParam();
                param.setRuleNum(rule.getRuleNum());
                param.setAuditorKind(kind);
                param.setAgentName(agent);
                param.setMinScore(rule.getMinScore());
                param.setMaxScore(rule.getMaxScore());
                param.setPassScore(rule.getPassScore());
                param.setStandard(rule.getStandard());
                param.setExcerpt(agentExcerpt(rule, context));
                ScoreResultDTO result = this.scoringClient.score(param);
                scores.add(RuleScoreVO.builder()
                        .ruleNum(rule.getRuleNum())
                        .score(result.getScore())
                        .rationale(result.getRationale())
                        .evidenceJson(result.getEvidenceJson())
                        .failed(Boolean.FALSE)
                        .build());
            } else {
                scores.add(this.ordinaryRuleEngine.score(rule, context));
            }
        }
        return scores;
    }

    private String agentExcerpt(RuleItemSnapshotVO rule, ScoreContextVO context) {
        JSONObject config = JSONUtil.parseObj(StrUtil.blankToDefault(rule.getEngineConfigJson(), "{}"));
        JSONArray keys = config.getJSONArray("paramKeys");
        JSONObject payload = new JSONObject();
        if (keys != null) {
            for (Object keyObj : keys) {
                String key = String.valueOf(keyObj);
                if ("Input".equals(key)) {
                    payload.set(key, context == null ? "" : StrUtil.nullToEmpty(context.getInputText()));
                } else if ("Attachment".equals(key)) {
                    JSONArray files = new JSONArray();
                    if (context != null && CollUtil.isNotEmpty(context.getAttachments())) {
                        for (AttachmentVO attachment : context.getAttachments()) {
                            JSONObject file = new JSONObject();
                            file.set("id", StrUtil.blankToDefault(attachment.getNum(), attachment.getFileName()));
                            file.set("name", attachment.getFileName());
                            file.set("url", StrUtil.blankToDefault(attachment.getFileUrl(), attachment.getObjectKey()));
                            files.add(file);
                        }
                    }
                    payload.set(key, files);
                } else {
                    JSONObject extras = JSONUtil.parseObj(context == null ? "{}"
                            : StrUtil.blankToDefault(context.getExtraParamsJson(), "{}"));
                    payload.set(key, extras.getStr(key));
                }
            }
        }
        return payload.toString();
    }

    @Override
    public String presignGet(String objectKey) {
        return this.objectStorageClient.presignGet(objectKey);
    }

    private RuleSetVersionEntity loadVersion(String versionNum) {
        RuleSetVersionEntity version = this.ruleSetVersionMapper.selectOne(
                new LambdaQueryWrapper<RuleSetVersionEntity>().eq(RuleSetVersionEntity::getNum, versionNum));
        if (version == null) {
            throw new BusinessException("规则集版本不存在");
        }
        return version;
    }

    private RuleSnapshotVO toSnapshot(String ruleSetNum, RuleSetVersionEntity version) {
        RuleSetEntity ruleSet = this.ruleSetMapper.selectOne(new LambdaQueryWrapper<RuleSetEntity>()
                .eq(RuleSetEntity::getNum, ruleSetNum));
        String scoreMode = ruleSet != null && StrUtil.isNotBlank(ruleSet.getScoreMode())
                ? ruleSet.getScoreMode() : version.getScoreMode();
        BigDecimal overallPassScore = ruleSet != null && ruleSet.getOverallPassScore() != null
                ? ruleSet.getOverallPassScore() : version.getOverallPassScore();
        List<RuleItemSnapshotVO> rules = new ArrayList<>();
        for (RuleItemEntity item : this.ruleItemMapper.selectList(new LambdaQueryWrapper<RuleItemEntity>()
                .eq(RuleItemEntity::getVersionNum, version.getNum())
                .orderByAsc(RuleItemEntity::getSortNo))) {
            AuditorEntity auditor = StrUtil.isBlank(item.getAuditorNum()) ? null
                    : this.auditorMapper.selectOne(new LambdaQueryWrapper<AuditorEntity>()
                    .eq(AuditorEntity::getNum, item.getAuditorNum()));
            rules.add(RuleItemSnapshotVO.builder()
                    .ruleNum(item.getNum())
                    .name(item.getName())
                    .standard(item.getStandard())
                    .minScore(item.getMinScore())
                    .maxScore(item.getMaxScore())
                    .passScore(item.getPassScore())
                    .weight(item.getWeight())
                    .veto(item.getVeto())
                    .engineKind(item.getEngineKind())
                    .engineConfigJson(item.getEngineConfigJson())
                    .auditorNum(item.getAuditorNum())
                    .auditorKind(auditor == null ? null : auditor.getKind())
                    .agentName(auditor == null ? null : auditor.getAgentName())
                    .build());
        }
        return RuleSnapshotVO.builder()
                .ruleSetNum(ruleSetNum)
                .ruleSetVersionNum(version.getNum())
                .versionNo(version.getVersionNo())
                .scoreMode(scoreMode)
                .overallPassScore(overallPassScore)
                .rules(rules)
                .build();
    }
}
