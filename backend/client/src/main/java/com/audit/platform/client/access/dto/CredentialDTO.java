package com.audit.platform.client.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CredentialDTO {
    private String num;
    private String name;
    private String keyPrefix;
    private Boolean enabled;
    private LocalDateTime createTime;
}
