package com.audit.platform.domain.ruleset.repository;

import com.audit.platform.domain.ruleset.RuleSet;

public interface RuleSetRepository {
    void save(RuleSet aggregate);

    RuleSet findByNum(String num);

    void deleteByNum(String num);
}
