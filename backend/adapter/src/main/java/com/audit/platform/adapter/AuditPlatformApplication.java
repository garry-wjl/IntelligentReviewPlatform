package com.audit.platform.adapter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.audit.platform")
@MapperScan("com.audit.platform.infra.**.mapper")
public class AuditPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditPlatformApplication.class, args);
    }
}
