package com.audit.platform.domain.evaluation.repository;

import com.audit.platform.domain.evaluation.Evaluation;

public interface EvaluationRepository {
    void save(Evaluation aggregate);

    Evaluation findByNum(String num);

    void deleteByNum(String num);
}
