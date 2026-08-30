package com.audit.platform.domain.access.repository;

import com.audit.platform.domain.access.Credential;

public interface CredentialRepository {
    void save(Credential aggregate);

    Credential findByNum(String num);

    void deleteByNum(String num);
}
