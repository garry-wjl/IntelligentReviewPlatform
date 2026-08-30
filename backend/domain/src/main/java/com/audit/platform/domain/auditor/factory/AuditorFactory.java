package com.audit.platform.domain.auditor.factory;

import com.audit.platform.domain.auditor.Auditor;

public interface AuditorFactory {
    /**
     * 按用户填写字段构建审核器。
     */
    Auditor create(String name, String kind, String agentNum, String description);

    Auditor createByNum(String num);
}
