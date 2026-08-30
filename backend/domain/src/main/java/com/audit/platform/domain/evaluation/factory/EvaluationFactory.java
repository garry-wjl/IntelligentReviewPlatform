package com.audit.platform.domain.evaluation.factory;

import com.audit.platform.domain.evaluation.Evaluation;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;

import java.util.List;

public interface EvaluationFactory {
    /**
     * 按调用方填写字段创建任务。
     */
    Evaluation create(String bizId, String auditorNum, String ruleSetNum, Boolean trial, List<AttachmentVO> attachments);

    Evaluation createByNum(String num);
}
