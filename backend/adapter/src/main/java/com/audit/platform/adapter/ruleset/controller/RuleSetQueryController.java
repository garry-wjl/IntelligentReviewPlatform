package com.audit.platform.adapter.ruleset.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.ruleset.RuleSetQueryService;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.ruleset.dto.RuleSetDTO;
import com.audit.platform.client.ruleset.dto.RuleSetDetailDTO;
import com.audit.platform.client.ruleset.dto.RuleSetNumParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetPageParamDTO;
import com.audit.platform.client.ruleset.dto.RuleSetVersionDTO;
import com.audit.platform.client.ruleset.dto.RuleSetVersionNumParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/ruleset/query")
public class RuleSetQueryController extends BaseController {
    @Resource
    private RuleSetQueryService ruleSetQueryService;

    @GetMapping("/page")
    public Result<PageDTO<RuleSetDTO>> page(RuleSetPageParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetQueryService.page(param));
    }

    @GetMapping("/detail")
    public Result<RuleSetDetailDTO> detail(RuleSetNumParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetQueryService.detail(param));
    }

    @GetMapping("/version")
    public Result<RuleSetVersionDTO> version(RuleSetVersionNumParamDTO param) {
        fillOperator(param);
        return Result.ok(ruleSetQueryService.versionDetail(param));
    }
}
