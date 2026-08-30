package com.audit.platform.domain.ruleset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.domain.ruleset.gateway.RuleSetGateway;
import com.audit.platform.domain.ruleset.repository.RuleSetRepository;
import com.audit.platform.domain.ruleset.valueobject.AuditorSnapshotVO;
import com.audit.platform.domain.ruleset.valueobject.RuleItemVO;
import com.audit.platform.domain.ruleset.valueobject.RuleSetVersionVO;
import com.audit.platform.domain.ruleset.valueobject.SceneSnapshotVO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import com.audit.platform.facade.domain.DomainEntity;
import com.audit.platform.facade.domain.DomainEventDTO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import com.audit.platform.facade.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 规则集聚合根。
 */
@Getter
@Setter
public class RuleSet extends DomainEntity {
    private String num;
    private String name;
    private String description;
    private String sceneNum;
    private String sceneName;
    private String sceneParamsJson;
    private Boolean enabled;
    private String scoreMode;
    private BigDecimal overallPassScore;
    private String currentPublishedVersionNum;
    private List<RuleSetVersionVO> versions;

    private RuleSetRepository ruleSetRepository;
    private RuleSetGateway ruleSetGateway;
    private DomainEventPublisher domainEventPublisher;

    public RuleSet() {
    }

    public RuleSet(String name, String description, String sceneNum, RuleSetRepository ruleSetRepository,
                   RuleSetGateway ruleSetGateway, DomainEventPublisher domainEventPublisher) {
        this.name = name;
        this.description = description;
        this.sceneNum = sceneNum;
        this.ruleSetRepository = ruleSetRepository;
        this.ruleSetGateway = ruleSetGateway;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 保存规则集。
     *
     * @param operatorId 操作人
     */
    @Override
    public void save(String operatorId) {
        // 1. 初始化对象
        this.initialize(operatorId);
        // 2. 领域规则校验
        Assert.notBlank(this.name, "规则集名称不能为空");
        Assert.notBlank(this.sceneNum, "必须选择场景");
        // 3. 赋值
        if (this.versions == null) {
            this.versions = new ArrayList<>();
        }
        if (this.enabled == null) {
            this.enabled = Boolean.TRUE;
        }
        if (StrUtil.isBlank(this.scoreMode)) {
            this.scoreMode = "VETO_WEIGHTED";
        }
        if (this.overallPassScore == null) {
            this.overallPassScore = new BigDecimal("70");
        }
        if (this.description == null) {
            this.description = "";
        }
        snapshotSceneIfNeeded();
        if (StrUtil.isBlank(this.num)) {
            this.num = this.ruleSetGateway.generateNum();
        }
        // 4. 领域完整性校验
        this.validate();
        // 5. 持久化对象
        this.ruleSetRepository.save(this);
        // 6. 发布领域事件
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
    }

    /**
     * 删除规则集。
     *
     * @param operatorId 操作人
     */
    @Override
    public void delete(String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(this.num, "规则集编号不能为空");
        this.validate();
        this.ruleSetRepository.deleteByNum(this.num);
        this.sendEvent(DomainEventConstant.RULE_SET_DELETED, operatorId);
    }

    @Override
    public void domainValidate() {
        Assert.notBlank(this.name, "规则集名称不能为空");
        Assert.notBlank(this.sceneNum, "必须选择场景");
        Assert.notNull(this.enabled, "启用状态不能为空");
        Assert.notBlank(this.scoreMode, "评估分方式不能为空");
        Assert.notNull(this.overallPassScore, "总分通过线不能为空");
    }

    /**
     * 更新名称与说明。
     *
     * @param name        名称
     * @param description 说明
     * @param operatorId  操作人
     */
    public void updateProfile(String name, String description, String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(name, "规则集名称不能为空");
        this.name = name;
        this.description = description == null ? "" : description;
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
    }

    /**
     * 启停规则集。
     *
     * @param enabled    是否启用
     * @param operatorId 操作人
     */
    public void setEnabledFlag(Boolean enabled, String operatorId) {
        this.initialize(operatorId);
        Assert.notNull(enabled, "启用状态不能为空");
        this.enabled = enabled;
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_ENABLED_CHANGED, operatorId);
    }

    /**
     * 创建草稿。
     *
     * @param basedOnVersionNum 基于的版本编码，可空；为空时自动继承当前发布（或最新已编号）版本
     * @param operatorId        操作人
     * @return 草稿版本编码
     */
    public String createDraft(String basedOnVersionNum, String operatorId) {
        this.initialize(operatorId);
        ensureVersions();
        boolean hasDraft = this.versions.stream().anyMatch(v -> "DRAFT".equals(v.getStatus()));
        Assert.isFalse(hasDraft, "已存在草稿，请先编辑或发布");
        RuleSetVersionVO draft = new RuleSetVersionVO();
        draft.setNum(this.ruleSetGateway.generateVersionNum());
        draft.setStatus("DRAFT");
        draft.setCurrentFlag(Boolean.FALSE);
        draft.setScoreMode(this.scoreMode);
        draft.setOverallPassScore(this.overallPassScore);
        draft.setRules(new ArrayList<>());
        String sourceNum = StrUtil.blankToDefault(basedOnVersionNum, resolveInheritVersionNum());
        if (StrUtil.isNotBlank(sourceNum)) {
            RuleSetVersionVO base = findVersion(sourceNum);
            draft.setBasedOnVersionNo(base.getVersionNo());
            List<RuleItemVO> copied = new ArrayList<>();
            if (CollUtil.isNotEmpty(base.getRules())) {
                for (RuleItemVO rule : base.getRules()) {
                    RuleItemVO copy = copyRule(rule);
                    copy.setId(null);
                    copy.setNum(this.ruleSetGateway.generateRuleNum());
                    copied.add(copy);
                }
            }
            draft.setRules(copied);
        }
        this.versions.add(draft);
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_DRAFT_CREATED, operatorId);
        return draft.getNum();
    }

    /**
     * 切换评估分方式。属于规则集基本信息，不随版本变化。
     *
     * @param scoreMode        评估分方式
     * @param overallPassScore 总分通过线
     * @param operatorId       操作人
     */
    public void changeScoreMode(String scoreMode, BigDecimal overallPassScore, String operatorId) {
        this.initialize(operatorId);
        Assert.notBlank(scoreMode, "评估分方式不能为空");
        Assert.isTrue(Set.of("ALL_PASS", "WEIGHTED_SUM", "VETO_WEIGHTED").contains(scoreMode), "评估分方式不合法");
        this.scoreMode = scoreMode;
        this.overallPassScore = overallPassScore == null ? BigDecimal.ZERO : overallPassScore;
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
    }

    /**
     * 新增或更新规则。
     *
     * @param versionNum 版本编码
     * @param rule       规则
     * @param operatorId 操作人
     * @return 规则编码
     */
    public String upsertRule(String versionNum, RuleItemVO rule, String operatorId) {
        this.initialize(operatorId);
        RuleSetVersionVO version = requireDraft(versionNum);
        if (version.getRules() == null) {
            version.setRules(new ArrayList<>());
        }
        Assert.notBlank(rule.getName(), "规则名称不能为空");
        Assert.notBlank(rule.getStandard(), "评审标准不能为空");
        normalizeEngine(rule);
        validateScoreContract(rule);
        validateEngine(rule);
        if (StrUtil.isBlank(rule.getNum())) {
            rule.setNum(this.ruleSetGateway.generateRuleNum());
            if (rule.getSortNo() == null) {
                rule.setSortNo(version.getRules().size() + 1);
            }
            version.getRules().add(rule);
        } else {
            RuleItemVO existed = version.getRules().stream()
                    .filter(item -> Objects.equals(item.getNum(), rule.getNum()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("规则不存在"));
            existed.setName(rule.getName());
            existed.setStandard(rule.getStandard());
            existed.setMinScore(rule.getMinScore());
            existed.setMaxScore(rule.getMaxScore());
            existed.setPassScore(rule.getPassScore());
            existed.setWeight(rule.getWeight());
            existed.setVeto(rule.getVeto());
            existed.setPositiveExample(rule.getPositiveExample());
            existed.setNegativeExample(rule.getNegativeExample());
            existed.setSortNo(rule.getSortNo());
            existed.setEngineKind(rule.getEngineKind());
            existed.setEngineConfigJson(rule.getEngineConfigJson());
            existed.setAuditorNum(rule.getAuditorNum());
            existed.setAuditorName(rule.getAuditorName());
        }
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
        return rule.getNum();
    }

    /**
     * 删除规则。
     *
     * @param versionNum 版本编码
     * @param ruleNum    规则编码
     * @param operatorId 操作人
     */
    public void removeRule(String versionNum, String ruleNum, String operatorId) {
        this.initialize(operatorId);
        RuleSetVersionVO version = requireDraft(versionNum);
        version.getRules().removeIf(item -> Objects.equals(item.getNum(), ruleNum));
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
    }

    /**
     * 调整规则顺序。
     *
     * @param versionNum 版本编码
     * @param ruleNum    规则编码
     * @param direction  -1 上移 / 1 下移
     * @param operatorId 操作人
     */
    public void moveRule(String versionNum, String ruleNum, Integer direction, String operatorId) {
        this.initialize(operatorId);
        RuleSetVersionVO version = requireDraft(versionNum);
        List<RuleItemVO> rules = version.getRules();
        rules.sort(Comparator.comparing(item -> item.getSortNo() == null ? 0 : item.getSortNo()));
        int index = -1;
        for (int i = 0; i < rules.size(); i++) {
            if (Objects.equals(rules.get(i).getNum(), ruleNum)) {
                index = i;
                break;
            }
        }
        Assert.isTrue(index >= 0, "规则不存在");
        int target = index + direction;
        Assert.isTrue(target >= 0 && target < rules.size(), "无法继续移动");
        RuleItemVO a = rules.get(index);
        RuleItemVO b = rules.get(target);
        Integer tmp = a.getSortNo();
        a.setSortNo(b.getSortNo());
        b.setSortNo(tmp);
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_SAVED, operatorId);
    }

    /**
     * 发布当前草稿。
     *
     * @param operatorId 操作人
     * @return 版本号
     */
    public Integer publish(String operatorId) {
        this.initialize(operatorId);
        ensureVersions();
        RuleSetVersionVO draft = this.versions.stream()
                .filter(v -> "DRAFT".equals(v.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("没有可发布的草稿"));
        validateForPublish(draft);
        int next = this.versions.stream()
                .map(RuleSetVersionVO::getVersionNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        for (RuleSetVersionVO version : this.versions) {
            if (Boolean.TRUE.equals(version.getCurrentFlag())) {
                version.setCurrentFlag(Boolean.FALSE);
                version.setStatus("ARCHIVED");
            }
        }
        draft.setStatus("PUBLISHED");
        draft.setCurrentFlag(Boolean.TRUE);
        draft.setVersionNo(next);
        this.currentPublishedVersionNum = draft.getNum();
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_PUBLISHED, operatorId);
        return next;
    }

    /**
     * 停用当前发布。
     *
     * @param operatorId 操作人
     */
    public void disableCurrentPublish(String operatorId) {
        this.initialize(operatorId);
        ensureVersions();
        boolean found = false;
        for (RuleSetVersionVO version : this.versions) {
            if (Boolean.TRUE.equals(version.getCurrentFlag())) {
                version.setCurrentFlag(Boolean.FALSE);
                found = true;
            }
        }
        Assert.isTrue(found, "没有当前已发布版本");
        this.currentPublishedVersionNum = null;
        this.validate();
        this.ruleSetRepository.save(this);
        this.sendEvent(DomainEventConstant.RULE_SET_PUBLISH_DISABLED, operatorId);
    }

    private void validateForPublish(RuleSetVersionVO draft) {
        Assert.isTrue(CollUtil.isNotEmpty(draft.getRules()), "至少需要一条规则");
        String mode = this.scoreMode;
        boolean needWeight = !"ALL_PASS".equals(mode);
        if (needWeight) {
            Assert.isTrue(this.overallPassScore != null && this.overallPassScore.compareTo(BigDecimal.ZERO) > 0,
                    "加权类模式需要配置总分通过线");
        }
        if ("VETO_WEIGHTED".equals(mode)) {
            boolean hasVeto = draft.getRules().stream().anyMatch(r -> Boolean.TRUE.equals(r.getVeto()));
            Assert.isTrue(hasVeto, "红线 + 加权模式至少需要一条红线规则");
        }
        for (RuleItemVO rule : draft.getRules()) {
            Assert.notBlank(rule.getAuditorNum(), "每条规则必须选择审核器");
            validateScoreContract(rule);
            if (needWeight) {
                Assert.isTrue(rule.getWeight() != null && rule.getWeight().compareTo(BigDecimal.ZERO) > 0,
                        "当前评估分方式需要填写大于 0 的权重");
            }
        }
    }

    private void validateScoreContract(RuleItemVO rule) {
        Assert.notNull(rule.getMinScore(), "最低分不能为空");
        Assert.notNull(rule.getMaxScore(), "最高分不能为空");
        Assert.notNull(rule.getPassScore(), "通过分不能为空");
        Assert.isTrue(rule.getMinScore().compareTo(rule.getMaxScore()) < 0, "最低分必须小于最高分");
        Assert.isTrue(rule.getPassScore().compareTo(rule.getMinScore()) >= 0
                && rule.getPassScore().compareTo(rule.getMaxScore()) <= 0, "通过分必须落在最低分与最高分之间");
    }

    private RuleSetVersionVO requireDraft(String versionNum) {
        RuleSetVersionVO version = findVersion(versionNum);
        Assert.isTrue("DRAFT".equals(version.getStatus()), "仅草稿可编辑");
        return version;
    }

    private String resolveInheritVersionNum() {
        if (StrUtil.isNotBlank(this.currentPublishedVersionNum)) {
            return this.currentPublishedVersionNum;
        }
        return this.versions.stream()
                .filter(v -> v.getVersionNo() != null)
                .max(Comparator.comparingInt(RuleSetVersionVO::getVersionNo))
                .map(RuleSetVersionVO::getNum)
                .orElse(null);
    }

    private RuleSetVersionVO findVersion(String versionNum) {
        ensureVersions();
        return this.versions.stream()
                .filter(v -> Objects.equals(v.getNum(), versionNum))
                .findFirst()
                .orElseThrow(() -> new BusinessException("规则集版本不存在"));
    }

    private void ensureVersions() {
        if (this.versions == null) {
            this.versions = new ArrayList<>();
        }
    }

    private RuleItemVO copyRule(RuleItemVO rule) {
        return RuleItemVO.builder()
                .name(rule.getName())
                .standard(rule.getStandard())
                .minScore(rule.getMinScore())
                .maxScore(rule.getMaxScore())
                .passScore(rule.getPassScore())
                .weight(rule.getWeight())
                .veto(rule.getVeto())
                .positiveExample(rule.getPositiveExample())
                .negativeExample(rule.getNegativeExample())
                .sortNo(rule.getSortNo())
                .engineKind(rule.getEngineKind())
                .engineConfigJson(rule.getEngineConfigJson())
                .auditorNum(rule.getAuditorNum())
                .auditorName(rule.getAuditorName())
                .build();
    }

    private void snapshotSceneIfNeeded() {
        SceneSnapshotVO snapshot = this.ruleSetGateway.loadScene(this.sceneNum);
        Assert.notNull(snapshot, "场景不存在");
        this.sceneName = snapshot.getSceneName();
        if (StrUtil.isBlank(this.sceneParamsJson)) {
            this.sceneParamsJson = snapshot.getParamsJson();
        }
    }

    private void normalizeEngine(RuleItemVO rule) {
        Assert.notBlank(rule.getAuditorNum(), "必须选择审核器");
        AuditorSnapshotVO auditor = this.ruleSetGateway.loadAuditor(rule.getAuditorNum());
        Assert.notNull(auditor, "审核器不存在");
        Assert.isTrue(Boolean.TRUE.equals(auditor.getEnabled()), "审核器已停用");
        rule.setAuditorName(auditor.getName());
        rule.setEngineKind("AGENT".equals(auditor.getKind()) ? "AGENT" : "ORDINARY");
        if (StrUtil.isBlank(rule.getEngineConfigJson())) {
            rule.setEngineConfigJson("AGENT".equals(rule.getEngineKind())
                    ? "{\"paramKeys\":[]}" : "{\"checks\":[]}");
        }
    }

    private void validateEngine(RuleItemVO rule) {
        Assert.isTrue("ORDINARY".equals(rule.getEngineKind()) || "AGENT".equals(rule.getEngineKind()),
                "规则器类型不合法");
        Set<String> allowed = SceneParamCatalog.keysOf(this.sceneParamsJson);
        JSONObject config = JSONUtil.parseObj(rule.getEngineConfigJson());
        if ("ORDINARY".equals(rule.getEngineKind())) {
            JSONArray checks = config.getJSONArray("checks");
            if (checks == null) {
                return;
            }
            for (Object item : checks) {
                JSONObject check = JSONUtil.parseObj(item);
                String paramKey = check.getStr("paramKey");
                String op = check.getStr("op");
                Assert.notBlank(paramKey, "校验参数不能为空");
                Assert.isTrue(allowed.contains(paramKey), "参数不在当前场景中：" + paramKey);
                Assert.notBlank(op, "校验操作不能为空");
                Assert.isTrue(Set.of("NOT_BLANK", "MIN_LENGTH", "MAX_LENGTH", "REGEX").contains(op),
                        "不支持的校验操作：" + op);
                if (!"NOT_BLANK".equals(op)) {
                    Assert.notBlank(check.getStr("value"), "该校验需要填写比较值");
                }
            }
            return;
        }
        JSONArray keys = config.getJSONArray("paramKeys");
        Assert.notNull(keys, "Agent 规则器必须选择要传入的参数");
        Assert.isTrue(!keys.isEmpty(), "Agent 规则器至少选择一个参数");
        for (Object key : keys) {
            String paramKey = String.valueOf(key);
            Assert.isTrue(allowed.contains(paramKey), "参数不在当前场景中：" + paramKey);
        }
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
