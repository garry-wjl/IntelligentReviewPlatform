package com.audit.platform.client.ruleset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleCheckDTO {
    private String paramKey;
    private String op;
    private String value;
}
