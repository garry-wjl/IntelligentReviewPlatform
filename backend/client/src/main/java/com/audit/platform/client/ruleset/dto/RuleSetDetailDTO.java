package com.audit.platform.client.ruleset.dto;

import com.audit.platform.client.scene.dto.SceneParamDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleSetDetailDTO {
    private String num;
    private String name;
    private String description;
    private String sceneNum;
    private String sceneName;
    @Builder.Default
    private List<SceneParamDTO> sceneParams = new ArrayList<>();
    private Boolean enabled;
    private String scoreMode;
    private java.math.BigDecimal overallPassScore;
    private String currentPublishedVersionNum;
    private Integer currentVersionNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Builder.Default
    private List<RuleSetVersionDTO> versions = new ArrayList<>();
}
