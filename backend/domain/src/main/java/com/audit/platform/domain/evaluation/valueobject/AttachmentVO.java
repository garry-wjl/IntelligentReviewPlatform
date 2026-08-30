package com.audit.platform.domain.evaluation.valueobject;

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
public class AttachmentVO {
    private Long id;
    private String num;
    private String objectKey;
    private String fileUrl;
    private String fileName;
    private String mime;
    private String role;
    private Integer sortNo;
    private Boolean parseFailed;
    private String excerpt;
}
