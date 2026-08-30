package com.audit.platform.domain.ruleset.gateway;

import com.audit.platform.domain.ruleset.valueobject.AuditorSnapshotVO;
import com.audit.platform.domain.ruleset.valueobject.SceneSnapshotVO;

public interface RuleSetGateway {
    String generateNum();

    String generateVersionNum();

    String generateRuleNum();

    SceneSnapshotVO loadScene(String sceneNum);

    AuditorSnapshotVO loadAuditor(String auditorNum);
}
