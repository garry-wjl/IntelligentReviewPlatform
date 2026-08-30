package com.audit.platform.domain.scene.factory;

import com.audit.platform.domain.scene.Scene;

public interface SceneFactory {
    /**
     * 按用户填写字段构建场景。
     */
    Scene create(String name, String description, String extraParamsJson);

    Scene createByNum(String num);
}
