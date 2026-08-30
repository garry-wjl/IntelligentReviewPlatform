package com.audit.platform.adapter.config;

import cn.hutool.core.io.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class LocalTosController {
    @Value("${audit.tos.local-dir:./data/tos}")
    private String localDir;

    @GetMapping("/internal/tos/{*objectKey}")
    public byte[] get(@PathVariable("objectKey") String objectKey) {
        File file = FileUtil.file(localDir, objectKey);
        if (!file.exists()) {
            return new byte[0];
        }
        return FileUtil.readBytes(file);
    }

    @PutMapping(value = "/internal/tos/{*objectKey}", consumes = MediaType.ALL_VALUE)
    public void put(@PathVariable("objectKey") String objectKey, @RequestBody(required = false) byte[] body) {
        File file = FileUtil.file(localDir, objectKey);
        FileUtil.mkParentDirs(file);
        FileUtil.writeBytes(body == null ? new byte[0] : body, file);
    }
}
