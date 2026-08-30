package com.audit.platform.domain.ruleset.valueobject;

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
public class SceneSnapshotVO {
    private String sceneNum;
    private String sceneName;
    private String paramsJson;
}
