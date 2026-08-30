package com.audit.platform.adapter.evaluation.controller;

import cn.hutool.core.util.StrUtil;
import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.evaluation.EvaluationQueryService;
import com.audit.platform.client.evaluation.dto.AttachmentUrlDTO;
import com.audit.platform.client.evaluation.dto.AttachmentUrlParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationBizQueryParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationDetailDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open/v1/evaluation/query")
public class OpenEvaluationQueryController extends BaseController {
    @Resource
    private EvaluationQueryService evaluationQueryService;

    @GetMapping("/detail")
    public Result<EvaluationDetailDTO> detail(@RequestParam(required = false) String num,
                                              @RequestParam(required = false) String bizId) {
        if (StrUtil.isNotBlank(num)) {
            EvaluationNumParamDTO param = new EvaluationNumParamDTO();
            param.setNum(num);
            fillOperator(param);
            return Result.ok(evaluationQueryService.detail(param));
        }
        EvaluationBizQueryParamDTO param = new EvaluationBizQueryParamDTO();
        param.setBizId(bizId);
        param.setCredentialNum(getCredentialNum());
        param.setTrial(Boolean.FALSE);
        fillOperator(param);
        return Result.ok(evaluationQueryService.findByBizId(param));
    }

    @GetMapping("/attachment-url")
    public Result<AttachmentUrlDTO> attachmentUrl(AttachmentUrlParamDTO param) {
        fillOperator(param);
        return Result.ok(evaluationQueryService.attachmentUrl(param));
    }
}
