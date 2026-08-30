package com.audit.platform.adapter.ruleset.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.ruleset.RuleSetCommandService;
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
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/ruleset/command")
public class RuleSetCommandController extends BaseController {
    @Resource
    private RuleSetCommandService ruleSetCommandService;

    @PostMapping("/create")
    public Result<NumDTO> create(@Valid @RequestBody RuleSetCreateParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetCommandService.create(param));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody RuleSetUpdateParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.updateProfile(param);
        return Result.ok(null);
    }

    @PostMapping("/set-enabled")
    public Result<Void> setEnabled(@Valid @RequestBody RuleSetEnabledParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.setEnabled(param);
        return Result.ok(null);
    }

    @PostMapping("/create-draft")
    public Result<VersionNumDTO> createDraft(@Valid @RequestBody RuleSetDraftParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetCommandService.createDraft(param));
    }

    @PostMapping("/score-mode")
    public Result<Void> scoreMode(@Valid @RequestBody RuleSetScoreModeParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.changeScoreMode(param);
        return Result.ok(null);
    }

    @PostMapping("/upsert-rule")
    public Result<RuleNumDTO> upsertRule(@Valid @RequestBody RuleUpsertParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetCommandService.upsertRule(param));
    }

    @PostMapping("/remove-rule")
    public Result<Void> removeRule(@Valid @RequestBody RuleRemoveParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.removeRule(param);
        return Result.ok(null);
    }

    @PostMapping("/move-rule")
    public Result<Void> moveRule(@Valid @RequestBody RuleMoveParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.moveRule(param);
        return Result.ok(null);
    }

    @PostMapping("/publish")
    public Result<VersionNumDTO> publish(@Valid @RequestBody RuleSetNumParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetCommandService.publish(param));
    }

    @PostMapping("/disable-current")
    public Result<Void> disableCurrent(@Valid @RequestBody RuleSetNumParamDTO param) {
        fillOperator(param);
        ruleSetCommandService.disableCurrent(param);
        return Result.ok(null);
    }
}
