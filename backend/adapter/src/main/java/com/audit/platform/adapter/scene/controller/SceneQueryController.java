package com.audit.platform.adapter.scene.controller;

import com.audit.platform.adapter.config.BaseController;
import com.audit.platform.application.scene.SceneQueryService;
import com.audit.platform.client.common.dto.EmptyParamDTO;
import com.audit.platform.client.common.dto.PageDTO;
import com.audit.platform.client.scene.dto.SceneDTO;
import com.audit.platform.client.scene.dto.SceneNumParamDTO;
import com.audit.platform.client.scene.dto.ScenePageParamDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/v1/scene/query")
public class SceneQueryController extends BaseController {
    @Resource
    private SceneQueryService sceneQueryService;

    @GetMapping("/page")
    public Result<PageDTO<SceneDTO>> page(ScenePageParamDTO param) {
        fillOperator(param);
        return Result.ok(sceneQueryService.page(param));
    }

    @GetMapping("/detail")
    public Result<SceneDTO> detail(SceneNumParamDTO param) {
        fillOperator(param);
        return Result.ok(sceneQueryService.detail(param));
    }

    @GetMapping("/list")
    public Result<List<SceneDTO>> list() {
        EmptyParamDTO param = new EmptyParamDTO();
        fillOperator(param);
        return Result.okData(sceneQueryService.listEnabled());
    }
}
