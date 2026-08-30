package com.audit.platform.domain.auditor.repository;

import com.audit.platform.domain.auditor.Auditor;

public interface AuditorRepository {
    void save(Auditor aggregate);

    Auditor findByNum(String num);

    void deleteByNum(String num);
}
