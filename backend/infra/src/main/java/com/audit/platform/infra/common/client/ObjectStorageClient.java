package com.audit.platform.infra.common.client;

/**
 * 对象存储客户端（本地 TOS Mock）。
 */
public interface ObjectStorageClient {
    String presignPut(String objectKey, String contentType);

    String presignGet(String objectKey);

    void put(String objectKey, byte[] body);

    byte[] get(String objectKey);
}
