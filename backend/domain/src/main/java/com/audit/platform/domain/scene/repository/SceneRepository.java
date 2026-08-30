package com.audit.platform.domain.scene.repository;

import com.audit.platform.domain.scene.Scene;

public interface SceneRepository {
    void save(Scene aggregate);

    Scene findByNum(String num);

    void deleteByNum(String num);
}
