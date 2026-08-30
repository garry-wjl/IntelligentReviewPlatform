package com.audit.platform.infra.common.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Component
public class LocalTosClient implements ObjectStorageClient {
    @Value("${audit.tos.local-dir:./data/tos}")
    private String localDir;

    @Override
    public String presignPut(String objectKey, String contentType) {
        return "http://localhost:8080/internal/tos/" + objectKey;
    }

    @Override
    public String presignGet(String objectKey) {
        return "http://localhost:8080/internal/tos/" + objectKey;
    }

    @Override
    public void put(String objectKey, byte[] body) {
        File file = FileUtil.file(localDir, objectKey);
        FileUtil.mkParentDirs(file);
        FileUtil.writeBytes(body == null ? new byte[0] : body, file);
    }

    @Override
    public byte[] get(String objectKey) {
        File file = FileUtil.file(localDir, objectKey);
        if (!file.exists()) {
            return StrUtil.blankToDefault(objectKey, "").getBytes(StandardCharsets.UTF_8);
        }
        return FileUtil.readBytes(file);
    }
}
