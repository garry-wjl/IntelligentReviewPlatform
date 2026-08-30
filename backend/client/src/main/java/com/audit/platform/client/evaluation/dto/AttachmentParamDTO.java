package com.audit.platform.client.evaluation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentParamDTO {
    private String objectKey;
    private String fileUrl;
    private String fileName;
    private String mime;
    private String role;
}
