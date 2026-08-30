package com.audit.platform.adapter.evaluation.listener;

import com.audit.platform.application.access.AccessCommandService;
import com.audit.platform.application.evaluation.EvaluationCommandService;
import com.audit.platform.client.access.dto.WebhookEnqueueParamDTO;
import com.audit.platform.client.evaluation.dto.EvaluationNumParamDTO;
import com.audit.platform.domain.common.DomainEventConstant;
import com.audit.platform.facade.domain.DomainEventDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EvaluationPipelineListener {
    @Resource
    private EvaluationCommandService evaluationCommandService;
    @Resource
    private AccessCommandService accessCommandService;

    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(DomainEventDTO event) {
        if (event == null || event.getType() == null) {
            return;
        }
        String num = event.getData() == null ? null : String.valueOf(event.getData());
        String operatorId = event.getSender() == null ? "system" : event.getSender();
        if (DomainEventConstant.EVALUATION_SAVED.equals(event.getType())) {
            EvaluationNumParamDTO param = new EvaluationNumParamDTO();
            param.setNum(num);
            param.setOperatorId(operatorId);
            evaluationCommandService.runPipeline(param);
            return;
        }
        String webhookEvent = mapWebhook(event.getType());
        if (webhookEvent != null) {
            WebhookEnqueueParamDTO param = new WebhookEnqueueParamDTO();
            param.setEvaluationNum(num);
            param.setEventName(webhookEvent);
            param.setOperatorId(operatorId);
            accessCommandService.enqueueWebhook(param);
        }
    }

    private String mapWebhook(String type) {
        if (DomainEventConstant.EVALUATION_SCORED.equals(type)) {
            return "evaluation.scored";
        }
        if (DomainEventConstant.EVALUATION_FINALIZED.equals(type)) {
            return "evaluation.finalized";
        }
        if (DomainEventConstant.EVALUATION_FAILED.equals(type)) {
            return "evaluation.failed";
        }
        return null;
    }
}
