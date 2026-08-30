package com.audit.platform.infra.evaluation.factory;

import com.audit.platform.domain.evaluation.Evaluation;
import com.audit.platform.domain.evaluation.factory.EvaluationFactory;
import com.audit.platform.domain.evaluation.gateway.EvaluationGateway;
import com.audit.platform.domain.evaluation.repository.EvaluationRepository;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvaluationFactoryImpl implements EvaluationFactory {

    @Resource
    private EvaluationRepository evaluationRepository;
    @Resource
    private EvaluationGateway evaluationGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public Evaluation create(String bizId, String auditorNum, String ruleSetNum, Boolean trial,
                             List<AttachmentVO> attachments) {
        return new Evaluation(bizId, auditorNum, ruleSetNum, trial, attachments, this.evaluationRepository,
                this.evaluationGateway, this.domainEventPublisher);
    }

    @Override
    public Evaluation createByNum(String num) {
        Evaluation evaluation = this.evaluationRepository.findByNum(num);
        if (evaluation == null) {
            return null;
        }
        evaluation.setEvaluationRepository(this.evaluationRepository);
        evaluation.setEvaluationGateway(this.evaluationGateway);
        evaluation.setDomainEventPublisher(this.domainEventPublisher);
        return evaluation;
    }
}
