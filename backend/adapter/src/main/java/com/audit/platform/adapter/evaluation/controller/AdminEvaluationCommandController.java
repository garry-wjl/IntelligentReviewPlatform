package com.audit.platform.adapter.evaluation.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.evaluation.EvaluationCommandService;
import com.audit.platform.client.evaluation.dto.EvaluationCreateParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationCreatedDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/evaluation/command")
public class AdminEvaluationCommandController extends BaseController {
    @Resource
    private EvaluationCommandService evaluationCommandService;

    @PostMapping("/trial")
    public Result<EvaluationCreatedDTO> trial(@Valid @RequestBody EvaluationCreateParamDTO param) {
        fillOperator(param);
        param.setTrial(Boolean.TRUE);
        return Result.ok(evaluationCommandService.create(param));
    }
}
