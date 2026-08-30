package com.audit.platform.adapter.auditor.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.auditor.AuditorCommandService;
import com.audit.platform.client.auditor.dto.AuditorCreateParamDTO;
import com.audit.platform.client.auditor.dto.AuditorEnabledParamDTO;
import com.audit.platform.client.auditor.dto.AuditorUpdateParamDTO;
import com.audit.platform.client.common.dto.CountDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.NumDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/auditor/command")
public class AuditorCommandController extends BaseController {
    @Resource
    private AuditorCommandService auditorCommandService;

    @PostMapping("/create")
    public Result<NumDTO> create(@Valid @RequestBody AuditorCreateParamDTO param) {
        fillOperator(param);
        return Result.ok(auditorCommandService.create(param));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody AuditorUpdateParamDTO param) {
        fillOperator(param);
        auditorCommandService.update(param);
        return Result.ok(null);
    }

    @PostMapping("/set-enabled")
    public Result<Void> setEnabled(@Valid @RequestBody AuditorEnabledParamDTO param) {
        fillOperator(param);
        auditorCommandService.setEnabled(param);
        return Result.ok(null);
    }

    @PostMapping("/sync-agents")
    public Result<CountDTO> syncAgents(@RequestBody(required = false) EmptyParamDTO param) {
        if (param == null) {
            param = new EmptyParamDTO();
        }
        fillOperator(param);
        return Result.ok(auditorCommandService.syncAgentCatalog(param));
    }
}
