package com.audit.platform.infra.scene.factory;

import com.audit.platform.domain.scene.Scene;
import com.audit.platform.domain.scene.factory.SceneFactory;
import com.audit.platform.domain.scene.gateway.SceneGateway;
import com.audit.platform.domain.scene.repository.SceneRepository;
import com.audit.platform.facade.domain.DomainEventPublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class SceneFactoryImpl implements SceneFactory {

    @Resource
    private SceneRepository sceneRepository;
    @Resource
    private SceneGateway sceneGateway;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public Scene create(String name, String description, String extraParamsJson) {
        return new Scene(name, description, extraParamsJson, this.sceneRepository, this.sceneGateway,
                this.domainEventPublisher);
    }

    @Override
    public Scene createByNum(String num) {
        Scene scene = this.sceneRepository.findByNum(num);
        if (scene == null) {
            return null;
        }
        scene.setSceneRepository(this.sceneRepository);
        scene.setSceneGateway(this.sceneGateway);
        scene.setDomainEventPublisher(this.domainEventPublisher);
        return scene;
    }
}
