package com.audit.platform.application.ruleset;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.audit.platform.client.ruleset.dto.RuleCheckDTO;
import com.audit.platform.client.ruleset.dto.RuleItemDTO;

import java.util.ArrayList;
import java.util.List;

public final class RuleEngineConfigHelper {
    private RuleEngineConfigHelper() {
    }

    public static String toJson(String engineKind, List<RuleCheckDTO> checks, List<String> agentParamKeys) {
        JSONObject config = new JSONObject();
        if ("AGENT".equals(engineKind)) {
            JSONArray keys = new JSONArray();
            if (CollUtil.isNotEmpty(agentParamKeys)) {
                for (String key : agentParamKeys) {
                    if (StrUtil.isNotBlank(key)) {
                        keys.add(key);
                    }
                }
            }
            config.set("paramKeys", keys);
        } else {
            JSONArray list = new JSONArray();
            if (CollUtil.isNotEmpty(checks)) {
                for (RuleCheckDTO check : checks) {
                    if (check == null || StrUtil.isBlank(check.getParamKey())) {
                        continue;
                    }
                    JSONObject item = new JSONObject();
                    item.set("paramKey", check.getParamKey());
                    item.set("op", check.getOp());
                    item.set("value", check.getValue());
                    list.add(item);
                }
            }
            config.set("checks", list);
        }
        return config.toString();
    }

    public static void fill(RuleItemDTO dto, String engineKind, String engineConfigJson) {
        String kind = StrUtil.blankToDefault(engineKind, "ORDINARY");
        dto.setEngineKind(kind);
        JSONObject config = JSONUtil.parseObj(StrUtil.blankToDefault(engineConfigJson, "{}"));
        if ("AGENT".equals(kind)) {
            List<String> keys = new ArrayList<>();
            JSONArray array = config.getJSONArray("paramKeys");
            if (array != null) {
                for (Object item : array) {
                    keys.add(String.valueOf(item));
                }
            }
            dto.setAgentParamKeys(keys);
            dto.setChecks(new ArrayList<>());
            return;
        }
        List<RuleCheckDTO> checks = new ArrayList<>();
        JSONArray array = config.getJSONArray("checks");
        if (array != null) {
            for (Object item : array) {
                JSONObject obj = JSONUtil.parseObj(item);
                RuleCheckDTO check = new RuleCheckDTO();
                check.setParamKey(obj.getStr("paramKey"));
                check.setOp(obj.getStr("op"));
                check.setValue(obj.getStr("value"));
                checks.add(check);
            }
        }
        dto.setChecks(checks);
        dto.setAgentParamKeys(new ArrayList<>());
    }
}
