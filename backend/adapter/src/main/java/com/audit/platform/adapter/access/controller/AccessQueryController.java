package com.audit.platform.adapter.access.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.access.AccessQueryService;
import com.audit.platform.client.access.dto.CredentialDTO;
import com.audit.platform.client.access.dto.CredentialPageParamDTO;
import com.audit.platform.client.access.dto.IntegrationDTO;
import com.audit.platform.client.access.dto.WebhookLogDTO;
import com.audit.platform.client.access.dto.WebhookLogQueryParamDTO;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/access/query")
public class AccessQueryController extends BaseController {
    @Resource
    private AccessQueryService accessQueryService;

    @GetMapping("/credentials")
    public Result<PageDTO<CredentialDTO>> credentials(CredentialPageParamDTO param) {
        fillOperator(param);
        return Result.ok(accessQueryService.pageCredentials(param));
    }

    @GetMapping("/integration")
    public Result<IntegrationDTO> integration() {
        EmptyParamDTO param = new EmptyParamDTO();
        fillOperator(param);
        return Result.ok(accessQueryService.getIntegration(param));
    }

    @GetMapping("/webhooks")
    public Result<PageDTO<WebhookLogDTO>> webhooks(WebhookLogQueryParamDTO param) {
        fillOperator(param);
        return Result.ok(accessQueryService.pageWebhookLogs(param));
    }
}
