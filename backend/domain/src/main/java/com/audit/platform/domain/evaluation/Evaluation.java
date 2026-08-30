package com.audit.platform.domain.evaluation;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.domain.evaluation.gateway.EvaluationGateway;
import com.audit.platform.domain.evaluation.repository.EvaluationRepository;
import com.audit.platform.domain.evaluation.valueobject.AnnotationVO;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.ClassifyResultVO;
import com.audit.platform.domain.evaluation.valueobject.RuleItemSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.RuleResultVO;
import com.audit.platform.domain.evaluation.valueobject.RuleScoreVO;
import com.audit.platform.domain.evaluation.valueobject.RuleSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.ScoreContextVO;
import com.audit.platform.domain.evaluation.valueobject.TimelineVO;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import com.audit.platform.facade.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 审核任务聚合根。
 */
@Getter
@Setter
public class Evaluation extends DomainEntity {
    private String num;
    private String bizId;
    private Boolean trial;
    private String status;
    private String auditorNum;
    private String auditorKind;
    private String agentName;
    private String ruleSetNum;
    private String ruleSetVersionNum;
    private Integer ruleSetVersionNo;
    private String ruleSetSource;
    private BigDecimal classifyConfidence;
    private String classifyReason;
    private String scoreMode;
    private BigDecimal overallPassScore;
    private BigDecimal totalScore;
    private Boolean passed;
    private Boolean complete;
    private String failReason;
    private String credentialNum;
    private String callbackUrl;
    private String inputText;
    private String extraParamsJson;
    private List<AttachmentVO> attachments;
    private List<RuleResultVO> results;
    private List<AnnotationVO> annotations;
    private List<TimelineVO> timeline;

    private EvaluationRepository evaluationRepository;
    private EvaluationGateway evaluationGateway;
    private DomainEventPublisher domainEventPublisher;

    public Evaluation() {
    }

    public Evaluation(String bizId, String auditorNum, String ruleSetNum, Boolean trial,
                      List<AttachmentVO> attachments, EvaluationRepository evaluationRepository,
                      EvaluationGateway evaluationGateway, DomainEventPublisher domainEventPublisher) {
        this.bizId = bizId;
        this.auditorNum = auditorNum;
        this.ruleSetNum = ruleSetNum;
        this.trial = trial;
        this.attachments = attachments;
        this.evaluationRepository = evaluationRepository;
        this.evaluationGateway = evaluationGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void save(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.bizId, "业务单号不能为空");
        if (StrUtil.isBlank(this.auditorNum)) {
            this.auditorNum = "-";
        }
        if (StrUtil.isBlank(this.auditorKind)) {
            this.auditorKind = "PER_RULE";
        }
        if (this.trial == null) {
            this.trial = Boolean.FALSE;
        }
        if (this.status == null) {
            this.status = "RECEIVED";
        }
        if (this.complete == null) {
            this.complete = Boolean.FALSE;
        }
        if (this.attachments == null) {
            this.attachments = new ArrayList<>();
        }
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }
        if (this.timeline == null) {
            this.timeline = new ArrayList<>();
        }
        Assert.isTrue(!this.attachments.isEmpty(), "至少上传 1 个附件");
        if (StrUtil.isBlank(this.num)) {
            this.num = this.evaluationGateway.generateNum(Boolean.TRUE.equals(this.trial));
        }
        ensureChildNums();
        for (AttachmentVO attachment : this.attachments) {
            if (StrUtil.isBlank(attachment.getObjectKey())) {
                attachment.setObjectKey("pending/" + attachment.getNum());
            }
        }
        if (StrUtil.isNotBlank(this.ruleSetNum)) {
            this.ruleSetSource = "SPECIFIED";
        }
        appendTimeline(operatorId, "创建审核任务", this.auditorNum);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_SAVED, operatorId);
    }

    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue(Boolean.TRUE.equals(this.trial), "仅试评任务可删除");
        this.validate();
        this.evaluationRepository.deleteByNum(this.num);
        this.sendEvent(DomainEventConstant.EVALUATION_DELETED, operatorId);
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.bizId, "业务单号不能为空");
        Assert.notBlank(this.status, "状态不能为空");
    }

    /**
     * 解析内容包。
     */
    public void startParse(String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue("RECEIVED".equals(this.status), "当前状态不允许解析");
        this.status = "PARSING";
        this.evaluationGateway.ingestRemoteFiles(this.attachments);
        this.evaluationGateway.parse(this.attachments);
        boolean allFailed = this.attachments.stream().allMatch(a -> Boolean.TRUE.equals(a.getParseFailed()));
        if (allFailed) {
            fail("全部附件解析失败", operatorId);
            return;
        }
        appendTimeline(operatorId, "解析完成", null);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_PARSED, operatorId);
    }

    /**
     * 匹配或加载规则集。
     */
    public void matchRuleSet(String operatorId, BigDecimal threshold) {
        this.initialize(operatorId);
        Assert.isTrue("PARSING".equals(this.status) || "RECEIVED".equals(this.status), "当前状态不允许匹配规则集");
        if (StrUtil.isBlank(this.ruleSetNum)) {
            this.status = "CLASSIFYING";
            ClassifyResultVO classified = this.evaluationGateway.classify(this.attachments);
            this.classifyConfidence = classified.getConfidence();
            this.classifyReason = classified.getReason();
            this.ruleSetNum = classified.getRuleSetNum();
            this.ruleSetSource = "CLASSIFIED";
            if (classified.getConfidence() == null || classified.getConfidence().compareTo(threshold) < 0) {
                this.status = "TYPE_PENDING";
                appendTimeline(operatorId, "识别置信度不足，停止打分", classified.getReason());
                this.validate();
                this.evaluationRepository.save(this);
                this.sendEvent(DomainEventConstant.EVALUATION_CLASSIFIED, operatorId);
                return;
            }
        }
        this.sendEvent(DomainEventConstant.EVALUATION_CLASSIFIED, operatorId);
        startScore(operatorId);
    }

    /**
     * 指定规则集并重评。
     */
    public void assignRuleSet(String ruleSetNum, String operatorId) {
        this.initialize(operatorId);
        assertMutable();
        Assert.isTrue("TYPE_PENDING".equals(this.status) || "SCORED".equals(this.status), "当前状态不允许指定规则集");
        Assert.notBlank(ruleSetNum, "规则集编号不能为空");
        this.ruleSetNum = ruleSetNum;
        this.ruleSetSource = "SPECIFIED";
        this.results = new ArrayList<>();
        this.passed = null;
        this.totalScore = null;
        this.complete = Boolean.FALSE;
        appendTimeline(operatorId, "指定规则集并重评", ruleSetNum);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_RECLASSIFIED, operatorId);
        startScore(operatorId);
    }

    /**
     * 开始逐条打分。
     */
    public void startScore(String operatorId) {
        this.initialize(operatorId);
        this.status = "SCORING";
        RuleSnapshotVO snapshot = Boolean.TRUE.equals(this.trial) && StrUtil.isNotBlank(this.ruleSetVersionNum)
                ? this.evaluationGateway.loadDraftRules(this.ruleSetVersionNum)
                : this.evaluationGateway.loadPublishedRules(this.ruleSetNum);
        this.ruleSetNum = snapshot.getRuleSetNum();
        this.ruleSetVersionNum = snapshot.getRuleSetVersionNum();
        this.ruleSetVersionNo = snapshot.getVersionNo();
        this.scoreMode = snapshot.getScoreMode();
        this.overallPassScore = snapshot.getOverallPassScore();
        List<RuleScoreVO> scores = this.evaluationGateway.score(snapshot, ScoreContextVO.builder()
                .inputText(this.inputText)
                .extraParamsJson(this.extraParamsJson)
                .attachments(this.attachments)
                .build(), this.auditorKind, this.agentName);
        applyMachineScores(snapshot, scores, operatorId);
    }

    /**
     * 人工改分。
     */
    public void patchScore(String ruleNum, BigDecimal score, String reason, String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue("SCORED".equals(this.status), "仅机评完成后可改分");
        RuleResultVO result = findResult(ruleNum);
        Assert.isFalse(Boolean.TRUE.equals(result.getFailed()), "失败规则不能改分");
        Assert.isTrue(score.compareTo(result.getMinScore()) >= 0 && score.compareTo(result.getMaxScore()) <= 0,
                "分数必须落在规则区间内");
        Assert.notBlank(reason, "改分原因不能为空");
        result.setHumanScore(score);
        result.setHumanReason(reason);
        summarize();
        appendTimeline(operatorId, "人工改分", ruleNum + " → " + score);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_UPDATED, operatorId);
    }

    /**
     * 添加标注。
     */
    public void addAnnotation(AnnotationVO annotation, String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue("SCORED".equals(this.status), "仅机评完成后可标注");
        Assert.notBlank(annotation.getContent(), "标注内容不能为空");
        if (this.annotations == null) {
            this.annotations = new ArrayList<>();
        }
        annotation.setNum(this.evaluationGateway.generateChildNum("ANN"));
        this.annotations.add(annotation);
        appendTimeline(operatorId, "添加标注", annotation.getContent());
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_UPDATED, operatorId);
    }

    /**
     * 锁定终态。
     */
    public void finalizeScore(String operatorId) {
        this.initialize(operatorId);
        Assert.isTrue("SCORED".equals(this.status), "仅 scored 可锁定");
        this.status = "FINALIZED";
        appendTimeline(operatorId, "锁定终态", null);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_FINALIZED, operatorId);
    }

    /**
     * 失败。
     */
    public void fail(String reason, String operatorId) {
        this.initialize(operatorId);
        this.status = "FAILED";
        this.failReason = reason;
        appendTimeline(operatorId, "任务失败", reason);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_FAILED, operatorId);
    }

    public void bindAuditorSnapshot(String auditorKind, String agentName) {
        this.auditorKind = auditorKind;
        this.agentName = agentName;
    }

    public void bindDraftVersion(String ruleSetVersionNum) {
        this.ruleSetVersionNum = ruleSetVersionNum;
    }

    public void bindCredential(String credentialNum, String callbackUrl) {
        this.credentialNum = credentialNum;
        this.callbackUrl = callbackUrl;
    }

    private void applyMachineScores(RuleSnapshotVO snapshot, List<RuleScoreVO> scores, String operatorId) {
        this.results = new ArrayList<>();
        for (RuleItemSnapshotVO rule : snapshot.getRules()) {
            RuleScoreVO score = scores.stream()
                    .filter(s -> Objects.equals(s.getRuleNum(), rule.getRuleNum()))
                    .findFirst()
                    .orElse(RuleScoreVO.builder().ruleNum(rule.getRuleNum()).failed(Boolean.TRUE).failReason("无打分结果").build());
            this.results.add(RuleResultVO.builder()
                    .num(this.evaluationGateway.generateChildNum("RES"))
                    .ruleNum(rule.getRuleNum())
                    .ruleName(rule.getName())
                    .standard(rule.getStandard())
                    .minScore(rule.getMinScore())
                    .maxScore(rule.getMaxScore())
                    .passScore(rule.getPassScore())
                    .weight(rule.getWeight())
                    .veto(rule.getVeto())
                    .machineScore(score.getScore())
                    .machineRationale(score.getRationale())
                    .failed(Boolean.TRUE.equals(score.getFailed()))
                    .failReason(score.getFailReason())
                    .evidenceJson(score.getEvidenceJson())
                    .build());
        }
        if (this.results.stream().allMatch(r -> Boolean.TRUE.equals(r.getFailed()))) {
            fail("全部规则打分失败", operatorId);
            return;
        }
        this.status = "SCORED";
        summarize();
        appendTimeline(operatorId, "机评完成", this.auditorKind);
        this.validate();
        this.evaluationRepository.save(this);
        this.sendEvent(DomainEventConstant.EVALUATION_SCORED, operatorId);
    }

    private void summarize() {
        this.complete = this.results.stream().noneMatch(r -> Boolean.TRUE.equals(r.getFailed()));
        String mode = this.scoreMode;
        if ("ALL_PASS".equals(mode)) {
            this.totalScore = null;
            this.passed = Boolean.TRUE.equals(this.complete) && this.results.stream().allMatch(this::rulePassed);
            return;
        }
        if ("WEIGHTED_SUM".equals(mode)) {
            BigDecimal total = BigDecimal.ZERO;
            for (RuleResultVO result : this.results) {
                BigDecimal display = displayScore(result);
                if (display != null && result.getWeight() != null) {
                    total = total.add(display.multiply(result.getWeight()));
                }
            }
            this.totalScore = total.setScale(2, RoundingMode.HALF_UP);
            this.passed = Boolean.TRUE.equals(this.complete) && this.totalScore.compareTo(nz(this.overallPassScore)) >= 0;
            return;
        }
        boolean vetoOk = this.results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getVeto()))
                .allMatch(r -> !Boolean.TRUE.equals(r.getFailed()) && rulePassed(r));
        BigDecimal total = BigDecimal.ZERO;
        boolean othersOk = true;
        for (RuleResultVO result : this.results) {
            if (Boolean.TRUE.equals(result.getVeto())) {
                continue;
            }
            if (Boolean.TRUE.equals(result.getFailed())) {
                othersOk = false;
            }
            BigDecimal display = displayScore(result);
            if (display != null && result.getWeight() != null) {
                total = total.add(display.multiply(result.getWeight()));
            }
        }
        this.totalScore = total.setScale(2, RoundingMode.HALF_UP);
        this.passed = vetoOk && othersOk && this.totalScore.compareTo(nz(this.overallPassScore)) >= 0;
        if (!Boolean.TRUE.equals(this.complete) && ("ALL_PASS".equals(mode) || !vetoOk)) {
            this.passed = Boolean.FALSE;
        }
    }

    private boolean rulePassed(RuleResultVO result) {
        BigDecimal display = displayScore(result);
        return display != null && display.compareTo(result.getPassScore()) >= 0;
    }

    private BigDecimal displayScore(RuleResultVO result) {
        if (Boolean.TRUE.equals(result.getFailed())) {
            return null;
        }
        return result.getHumanScore() != null ? result.getHumanScore() : result.getMachineScore();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private RuleResultVO findResult(String ruleNum) {
        return this.results.stream()
                .filter(r -> Objects.equals(r.getRuleNum(), ruleNum))
                .findFirst()
                .orElseThrow(() -> new BusinessException("规则结果不存在"));
    }

    private void assertMutable() {
        Assert.isFalse("FINALIZED".equals(this.status), "已锁定，不可改");
    }

    private void ensureChildNums() {
        int i = 1;
        for (AttachmentVO attachment : this.attachments) {
            if (StrUtil.isBlank(attachment.getNum())) {
                attachment.setNum(this.evaluationGateway.generateChildNum("FILE"));
            }
            if (StrUtil.isBlank(attachment.getRole())) {
                attachment.setRole(i == 1 ? "main" : "appendix");
            }
            attachment.setSortNo(i++);
        }
    }

    private void appendTimeline(String operatorId, String title, String detail) {
        if (this.timeline == null) {
            this.timeline = new ArrayList<>();
        }
        this.timeline.add(TimelineVO.builder()
                .num(this.evaluationGateway.generateChildNum("TL"))
                .actor(operatorId)
                .title(title)
                .detail(detail)
                .build());
    }

    private void sendEvent(String type, String operatorId) {
        this.domainEventPublisher.send(DomainEventDTO.builder()
                .id(IdUtil.fastSimpleUUID())
                .type(type)
                .data(this.num)
                .time(System.currentTimeMillis())
                .sender(operatorId)
                .build());
    }
}
