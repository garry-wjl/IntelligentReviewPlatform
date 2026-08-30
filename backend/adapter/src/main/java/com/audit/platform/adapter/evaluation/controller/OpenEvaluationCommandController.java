package com.audit.platform.adapter.evaluation.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.access.AccessCommandService;
import com.audit.platform.application.evaluation.EvaluationCommandService;
import com.audit.platform.client.access.dto.PresignDTO;
import com.audit.platform.client.access.dto.PresignParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationAnnotateParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationAssignParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationCreateParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationCreatedDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationScoreParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open/v1/evaluation/command")
public class OpenEvaluationCommandController extends BaseController {
    @Resource
    private EvaluationCommandService evaluationCommandService;
    @Resource
    private AccessCommandService accessCommandService;

    @PostMapping("/presign")
    public Result<PresignDTO> presign(@Valid @RequestBody PresignParamDTO param) {
        fillOperator(param);
        return Result.ok(accessCommandService.presignUpload(param));
    }

    @PostMapping("/create")
    public Result<EvaluationCreatedDTO> create(@Valid @RequestBody EvaluationCreateParamDTO param) {
        fillOperator(param);
        param.setCredentialNum(getCredentialNum());
        param.setTrial(Boolean.FALSE);
        return Result.ok(evaluationCommandService.create(param));
    }

    @PostMapping("/score")
    public Result<Void> score(@Valid @RequestBody EvaluationScoreParamDTO param) {
        fillOperator(param);
        evaluationCommandService.patchScore(param);
        return Result.ok(null);
    }

    @PostMapping("/annotate")
    public Result<Void> annotate(@Valid @RequestBody EvaluationAnnotateParamDTO param) {
        fillOperator(param);
        evaluationCommandService.annotate(param);
        return Result.ok(null);
    }

    @PostMapping("/finalize")
    public Result<Void> finalizeScore(@Valid @RequestBody EvaluationNumParamDTO param) {
        fillOperator(param);
        evaluationCommandService.finalizeScore(param);
        return Result.ok(null);
    }

    @PostMapping("/assign-ruleset")
    public Result<Void> assign(@Valid @RequestBody EvaluationAssignParamDTO param) {
        fillOperator(param);
        evaluationCommandService.assignRuleSet(param);
        return Result.ok(null);
    }
}
