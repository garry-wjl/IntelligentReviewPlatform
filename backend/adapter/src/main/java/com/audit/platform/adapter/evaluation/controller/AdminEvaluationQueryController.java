package com.audit.platform.adapter.evaluation.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.evaluation.EvaluationQueryService;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.evaluation.dto.AttachmentUrlDTO;
import com.audit.platform.client.evaluation.dto.AttachmentUrlParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationDetailDTO;
import com.audit.platform.client.evaluation.dto.EvaluationListDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationPageParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/evaluation/query")
public class AdminEvaluationQueryController extends BaseController {
    @Resource
    private EvaluationQueryService evaluationQueryService;

    @GetMapping("/page")
    public Result<PageDTO<EvaluationListDTO>> page(EvaluationPageParamDTO param) {
        fillOperator(param);
        return Result.ok(evaluationQueryService.page(param));
    }

    @GetMapping("/detail")
    public Result<EvaluationDetailDTO> detail(EvaluationNumParamDTO param) {
        fillOperator(param);
        return Result.ok(evaluationQueryService.detail(param));
    }

    @GetMapping("/attachment-url")
    public Result<AttachmentUrlDTO> attachmentUrl(AttachmentUrlParamDTO param) {
        fillOperator(param);
        return Result.ok(evaluationQueryService.attachmentUrl(param));
    }
}
