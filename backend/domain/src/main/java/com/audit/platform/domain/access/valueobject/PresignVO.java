package com.audit.platform.domain.access.valueobject;

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
public class PresignVO {
    private String objectKey;
    private String uploadUrl;
    private String method;
}
