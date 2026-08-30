package com.audit.platform.infra.ruleset.factory;

import com.audit.platform.domain.ruleset.RuleSet;
import com.audit.platform.domain.ruleset.factory.RuleSetFactory;
import com.audit.platform.domain.ruleset.gateway.RuleSetGateway;
import com.audit.platform.domain.ruleset.repository.RuleSetRepository;
import com.audit.platform.facade.domain.DomainEventPublisher;
import com.audit.platform.facade.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuleSetFactoryImpl implements RuleSetFactory {
    @Resource
    private RuleSetRepository ruleSetRepository;
    @Resource
    private RuleSetGateway ruleSetGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public RuleSet create(String name, String description, String sceneNum) {
        return new RuleSet(name, description, sceneNum, ruleSetRepository, ruleSetGateway, domainEventPublisher);
    }

    @Override
    public RuleSet createByNum(String num) {
        RuleSet ruleSet = ruleSetRepository.findByNum(num);
        if (ruleSet == null) {
            throw new BusinessException("规则集不存在");
        }
        ruleSet.setRuleSetRepository(ruleSetRepository);
        ruleSet.setRuleSetGateway(ruleSetGateway);
        ruleSet.setDomainEventPublisher(domainEventPublisher);
        return ruleSet;
    }
}
