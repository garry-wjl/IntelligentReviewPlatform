package com.audit.platform.client.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO<T> {
    private Long total;
    private Integer pageNo;
    private Integer pageSize;
    @Builder.Default
    private List<T> list = new ArrayList<>();
}
