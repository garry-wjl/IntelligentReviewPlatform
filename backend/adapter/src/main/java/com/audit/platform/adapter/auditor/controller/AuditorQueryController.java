package com.audit.platform.adapter.auditor.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.auditor.AuditorQueryService;
import com.audit.platform.client.auditor.dto.AgentOptionDTO;
import com.audit.platform.client.auditor.dto.AuditorDTO;
import com.audit.platform.client.auditor.dto.AuditorNumParamDTO;
import com.audit.platform.client.auditor.dto.AuditorPageParamDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/v1/auditor/query")
public class AuditorQueryController extends BaseController {
    @Resource
    private AuditorQueryService auditorQueryService;

    @GetMapping("/page")
    public Result<PageDTO<AuditorDTO>> page(AuditorPageParamDTO param) {
        fillOperator(param);
        return Result.ok(auditorQueryService.page(param));
    }

    @GetMapping("/detail")
    public Result<AuditorDTO> detail(AuditorNumParamDTO param) {
        fillOperator(param);
        return Result.ok(auditorQueryService.detail(param));
    }

    @GetMapping("/agents")
    public Result<List<AgentOptionDTO>> agents() {
        EmptyParamDTO param = new EmptyParamDTO();
        fillOperator(param);
        return Result.okData(auditorQueryService.listAgents(param));
    }
}
