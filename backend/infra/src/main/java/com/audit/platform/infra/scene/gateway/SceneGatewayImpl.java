package com.audit.platform.infra.scene.gateway;

import com.audit.platform.domain.scene.gateway.SceneGateway;
import com.audit.platform.infra.common.util.NumGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SceneGatewayImpl implements SceneGateway {

    @Resource
    private NumGenerator numGenerator;

    @Override
    public String generateNum() {
        return this.numGenerator.next("SCN-", 4);
    }
}
