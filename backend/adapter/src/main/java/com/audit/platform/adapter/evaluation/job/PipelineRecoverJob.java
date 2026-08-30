package com.audit.platform.adapter.evaluation.job;

import com.audit.platform.application.evaluation.EvaluationCommandService;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PipelineRecoverJob {
    @Resource
    private EvaluationCommandService evaluationCommandService;

    @Scheduled(fixedDelay = 30000)
    public void recover() {
        EmptyParamDTO param = new EmptyParamDTO();
        param.setOperatorId("system");
        evaluationCommandService.recoverStuck(param);
    }
}
