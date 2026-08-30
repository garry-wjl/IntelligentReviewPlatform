package com.audit.platform.client.access.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CredentialSecretDTO {
    private String num;
    private String name;
    private String keyPrefix;
    private String rawSecret;
}
