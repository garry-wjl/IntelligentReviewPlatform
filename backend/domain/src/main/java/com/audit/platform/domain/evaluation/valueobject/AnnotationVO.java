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
public class AnnotationVO {
    private Long id;
    private String num;
    private String target;
    private String ruleNum;
    private String fileNum;
    private String location;
    private String content;
}
