package com.audit.platform.domain.access.factory;

import com.audit.platform.domain.access.Credential;

public interface CredentialFactory {
    /**
     * 按名称创建凭证。
     *
     * @param name 名称
     * @return 未持久化凭证
     */
    Credential create(String name);

    /**
     * 按业务编码加载。
     *
     * @param num 凭证编号
     * @return 凭证
     */
    Credential createByNum(String num);

    /**
     * 按密钥前缀加载（开放 API 鉴权）。
     *
     * @param keyPrefix 密钥前缀
     * @return 凭证
     */
    Credential createByKeyPrefix(String keyPrefix);
}
