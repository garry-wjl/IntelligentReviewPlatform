package com.audit.platform.adapter.access.job;

import com.audit.platform.application.access.AccessCommandService;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookDispatchJob {
    @Resource
    private AccessCommandService accessCommandService;

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        EmptyParamDTO param = new EmptyParamDTO();
        param.setOperatorId("system");
        accessCommandService.dispatchDue(param);
    }
}
