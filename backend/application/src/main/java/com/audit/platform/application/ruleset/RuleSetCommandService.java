package com.audit.platform.application.ruleset;

import com.audit.platform.client.common.dto.NumDTO;
import com.audit.platform.client.ruleset.dto.RuleMoveParamDTO;
import com.audit.platform.client.ruleset.dto.RuleNumDTO;
import com.audit.platform.client.ruleset.dto.RuleRemoveParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetCreateParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetDraftParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetEnabledParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetNumParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetScoreModeParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetUpdateParamDTO;
import com.audit.platform.client.ruleset.dto.RuleUpsertParamDTO;
import com.audit.platform.client.ruleset.dto.VersionNumDTO;
import com.audit.platform.domain.ruleset.RuleSet;
import com.audit.platform.domain.ruleset.factory.RuleSetFactory;
import com.audit.platform.domain.ruleset.valueobject.RuleItemVO;
import com.audit.platform.infra.common.constant.LockKeyConstant;
import com.audit.platform.infra.common.lock.RedisLockHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleSetCommandService {
    @Resource
    private RuleSetFactory ruleSetFactory;
    @Resource
    private RedisLockHelper redisLockHelper;

    @Transactional(rollbackFor = Exception.class)
    public NumDTO create(RuleSetCreateParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.RULE_SET + "create:" + param.getName(), () -> {
            RuleSet ruleSet = ruleSetFactory.create(param.getName(), param.getDescription(), param.getSceneNum());
            ruleSet.save(param.getOperatorId());
            ruleSet.createDraft(null, param.getOperatorId());
            return NumDTO.builder().num(ruleSet.getNum()).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(RuleSetUpdateParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.updateProfile(param.getName(), param.getDescription(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(RuleSetEnabledParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.setEnabledFlag(param.getEnabled(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public VersionNumDTO createDraft(RuleSetDraftParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            String versionNum = ruleSet.createDraft(param.getBasedOnVersionNum(), param.getOperatorId());
            return VersionNumDTO.builder().versionNum(versionNum).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeScoreMode(RuleSetScoreModeParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.changeScoreMode(param.getScoreMode(), param.getOverallPassScore(),
                    param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public RuleNumDTO upsertRule(RuleUpsertParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            RuleItemVO rule = RuleItemVO.builder()
                    .num(param.getRuleNum())
                    .name(param.getName())
                    .standard(param.getStandard())
                    .minScore(param.getMinScore())
                    .maxScore(param.getMaxScore())
                    .passScore(param.getPassScore())
                    .weight(param.getWeight())
                    .veto(param.getVeto())
                    .positiveExample(param.getPositiveExample())
                    .negativeExample(param.getNegativeExample())
                    .sortNo(param.getSortNo())
                    .engineKind(param.getEngineKind())
                    .auditorNum(param.getAuditorNum())
                    .engineConfigJson(RuleEngineConfigHelper.toJson(param.getEngineKind(), param.getChecks(),
                            param.getAgentParamKeys()))
                    .build();
            String ruleNum = ruleSet.upsertRule(param.getVersionNum(), rule, param.getOperatorId());
            return RuleNumDTO.builder().ruleNum(ruleNum).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeRule(RuleRemoveParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.removeRule(param.getVersionNum(), param.getRuleNum(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveRule(RuleMoveParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.moveRule(param.getVersionNum(), param.getRuleNum(), param.getDirection(), param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public VersionNumDTO publish(RuleSetNumParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            Integer versionNo = ruleSet.publish(param.getOperatorId());
            return VersionNumDTO.builder().versionNo(versionNo).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableCurrent(RuleSetNumParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.RULE_SET + param.getNum(), () -> {
            RuleSet ruleSet = ruleSetFactory.createByNum(param.getNum());
            ruleSet.disableCurrentPublish(param.getOperatorId());
        });
    }
}
