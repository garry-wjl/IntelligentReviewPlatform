package com.audit.platform.client.evaluation.dto;

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
public class TimelineDTO {
    private String num;
    private String actor;
    private String title;
    private String detail;
    private LocalDateTime createTime;
}
