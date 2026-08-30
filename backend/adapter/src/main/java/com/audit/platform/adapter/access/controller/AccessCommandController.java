package com.audit.platform.adapter.access.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.access.AccessCommandService;
import com.audit.platform.client.access.dto.CredentialCreateParamDTO;
import com.audit.platform.client.access.dto.CredentialNumParamDTO;
import com.audit.platform.client.access.dto.CredentialSecretDTO;
import com.audit.platform.client.access.dto.IntegrationUpdateParamDTO;
import com.audit.platform.client.access.dto.WebhookNumParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/access/command")
public class AccessCommandController extends BaseController {
    @Resource
    private AccessCommandService accessCommandService;

    @PostMapping("/create-credential")
    public Result<CredentialSecretDTO> createCredential(@Valid @RequestBody CredentialCreateParamDTO param) {
        fillOperator(param);
        return Result.ok(accessCommandService.createCredential(param));
    }

    @PostMapping("/disable-credential")
    public Result<Void> disableCredential(@Valid @RequestBody CredentialNumParamDTO param) {
        fillOperator(param);
        accessCommandService.disableCredential(param);
        return Result.ok(null);
    }

    @PostMapping("/update-integration")
    public Result<Void> updateIntegration(@Valid @RequestBody IntegrationUpdateParamDTO param) {
        fillOperator(param);
        accessCommandService.updateIntegration(param);
        return Result.ok(null);
    }

    @PostMapping("/replay-webhook")
    public Result<Void> replayWebhook(@Valid @RequestBody WebhookNumParamDTO param) {
        fillOperator(param);
        accessCommandService.replayDead(param);
        return Result.ok(null);
    }
}
