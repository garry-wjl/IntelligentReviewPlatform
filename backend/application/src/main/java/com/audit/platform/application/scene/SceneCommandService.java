package com.audit.platform.application.scene;

import cn.hutool.json.JSONUtil;
import com.audit.platform.client.common.dto.NumDTO;
import com.audit.platform.client.scene.dto.SceneCreateParamDTO;
import com.audit.platform.client.scene.dto.SceneEnabledParamDTO;
import com.audit.platform.client.scene.dto.SceneExtraParamDTO;
import com.audit.platform.client.scene.dto.SceneUpdateParamDTO;
import com.audit.platform.domain.scene.Scene;
import com.audit.platform.domain.scene.factory.SceneFactory;
import com.audit.platform.facade.exception.BusinessException;
import com.audit.platform.infra.common.constant.LockKeyConstant;
import com.audit.platform.infra.common.lock.RedisLockHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SceneCommandService {
    @Resource
    private SceneFactory sceneFactory;
    @Resource
    private RedisLockHelper redisLockHelper;

    @Transactional(rollbackFor = Exception.class)
    public NumDTO create(SceneCreateParamDTO param) {
        return redisLockHelper.execute(LockKeyConstant.SCENE + "create:" + param.getName(), () -> {
            Scene scene = sceneFactory.create(param.getName(), param.getDescription(), extraJson(param.getExtraParams()));
            scene.save(param.getOperatorId());
            return NumDTO.builder().num(scene.getNum()).build();
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SceneUpdateParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.SCENE + param.getNum(), () -> {
            Scene scene = require(param.getNum());
            scene.updateProfile(param.getName(), param.getDescription(), extraJson(param.getExtraParams()),
                    param.getOperatorId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(SceneEnabledParamDTO param) {
        redisLockHelper.execute(LockKeyConstant.SCENE + param.getNum(), () -> {
            Scene scene = require(param.getNum());
            scene.setEnabledFlag(param.getEnabled(), param.getOperatorId());
        });
    }

    private Scene require(String num) {
        Scene scene = sceneFactory.createByNum(num);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }
        return scene;
    }

    private String extraJson(List<SceneExtraParamDTO> extras) {
        List<SceneExtraParamDTO> list = extras == null ? new ArrayList<>() : extras;
        return JSONUtil.toJsonStr(list);
    }
}
