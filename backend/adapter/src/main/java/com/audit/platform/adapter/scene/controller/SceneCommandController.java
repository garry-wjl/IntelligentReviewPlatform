package com.audit.platform.adapter.scene.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.scene.SceneCommandService;
import com.audit.platform.client.common.dto.NumDTO;
import com.audit.platform.client.scene.dto.SceneCreateParamDTO;
import com.audit.platform.client.scene.dto.SceneEnabledParamDTO;
import com.audit.platform.client.scene.dto.SceneUpdateParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/scene/command")
public class SceneCommandController extends BaseController {
    @Resource
    private SceneCommandService sceneCommandService;

    @PostMapping("/create")
    public Result<NumDTO> create(@Valid @RequestBody SceneCreateParamDTO param) {
        fillOperator(param);
        return Result.ok(sceneCommandService.create(param));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody SceneUpdateParamDTO param) {
        fillOperator(param);
        sceneCommandService.update(param);
        return Result.ok(null);
    }

    @PostMapping("/set-enabled")
    public Result<Void> setEnabled(@Valid @RequestBody SceneEnabledParamDTO param) {
        fillOperator(param);
        sceneCommandService.setEnabled(param);
        return Result.ok(null);
    }
}
