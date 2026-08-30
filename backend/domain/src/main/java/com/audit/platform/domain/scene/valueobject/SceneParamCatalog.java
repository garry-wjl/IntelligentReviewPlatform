package com.audit.platform.domain.scene.valueobject;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 场景参数目录：每个场景固定带 Input、Attachment，再叠加用户扩展参数。
 */
public final class SceneParamCatalog {
    public static final String INPUT = "Input";
    public static final String ATTACHMENT = "Attachment";
    public static final String TYPE_INPUT = "INPUT";
    public static final String TYPE_ATTACHMENT = "ATTACHMENT";
    public static final String TYPE_STRING = "STRING";

    private SceneParamCatalog() {
    }

    public static List<SceneParamVO> builtins() {
        List<SceneParamVO> list = new ArrayList<>();
        list.add(SceneParamVO.builder().key(INPUT).label("用户输入").type(TYPE_INPUT).builtin(Boolean.TRUE).build());
        list.add(SceneParamVO.builder().key(ATTACHMENT).label("附件").type(TYPE_ATTACHMENT).builtin(Boolean.TRUE).build());
        return list;
    }

    public static List<SceneParamVO> parseExtra(String extraParamsJson) {
        List<SceneParamVO> extras = new ArrayList<>();
        if (StrUtil.isBlank(extraParamsJson) || "[]".equals(extraParamsJson.trim())) {
            return extras;
        }
        JSONArray array = JSONUtil.parseArray(extraParamsJson);
        for (Object item : array) {
            JSONObject obj = JSONUtil.parseObj(item);
            extras.add(SceneParamVO.builder()
                    .key(StrUtil.trim(obj.getStr("key")))
                    .label(StrUtil.blankToDefault(StrUtil.trim(obj.getStr("label")), StrUtil.trim(obj.getStr("key"))))
                    .type(TYPE_STRING)
                    .builtin(Boolean.FALSE)
                    .build());
        }
        return extras;
    }

    public static List<SceneParamVO> resolve(String extraParamsJson) {
        List<SceneParamVO> params = new ArrayList<>(builtins());
        params.addAll(parseExtra(extraParamsJson));
        return params;
    }

    public static String toExtraJson(List<SceneParamVO> extras) {
        JSONArray array = new JSONArray();
        if (CollUtil.isNotEmpty(extras)) {
            for (SceneParamVO extra : extras) {
                if (extra == null || StrUtil.isBlank(extra.getKey())) {
                    continue;
                }
                JSONObject obj = new JSONObject();
                obj.set("key", extra.getKey().trim());
                obj.set("label", StrUtil.blankToDefault(extra.getLabel(), extra.getKey()).trim());
                obj.set("type", TYPE_STRING);
                array.add(obj);
            }
        }
        return array.toString();
    }

    public static String toResolvedJson(String extraParamsJson) {
        JSONArray array = new JSONArray();
        for (SceneParamVO param : resolve(extraParamsJson)) {
            JSONObject obj = new JSONObject();
            obj.set("key", param.getKey());
            obj.set("label", param.getLabel());
            obj.set("type", param.getType());
            obj.set("builtin", Boolean.TRUE.equals(param.getBuiltin()));
            array.add(obj);
        }
        return array.toString();
    }

    public static void validateExtra(String extraParamsJson) {
        List<SceneParamVO> extras = parseExtra(extraParamsJson);
        Set<String> keys = new HashSet<>();
        for (SceneParamVO extra : extras) {
            Assert.notBlank(extra.getKey(), "扩展参数键不能为空");
            String key = extra.getKey().trim();
            Assert.isTrue(key.matches("^[A-Za-z][A-Za-z0-9_]*$"), "扩展参数键须以字母开头，仅含字母数字下划线");
            String lower = key.toLowerCase(Locale.ROOT);
            Assert.isFalse(INPUT.equalsIgnoreCase(key) || ATTACHMENT.equalsIgnoreCase(key),
                    "扩展参数不能使用内置键 Input / Attachment");
            Assert.isTrue(keys.add(lower), "扩展参数键不能重复：" + key);
            Assert.notBlank(extra.getLabel(), "扩展参数名称不能为空");
        }
    }

    public static Set<String> keysOf(String resolvedParamsJson) {
        Set<String> keys = new HashSet<>();
        for (SceneParamVO param : resolveFromResolvedJson(resolvedParamsJson)) {
            if (StrUtil.isNotBlank(param.getKey())) {
                keys.add(param.getKey());
            }
        }
        return keys;
    }

    public static List<SceneParamVO> resolveFromResolvedJson(String resolvedParamsJson) {
        if (StrUtil.isBlank(resolvedParamsJson)) {
            return resolve(null);
        }
        JSONArray array = JSONUtil.parseArray(resolvedParamsJson);
        List<SceneParamVO> list = new ArrayList<>();
        for (Object item : array) {
            JSONObject obj = JSONUtil.parseObj(item);
            list.add(SceneParamVO.builder()
                    .key(obj.getStr("key"))
                    .label(obj.getStr("label"))
                    .type(obj.getStr("type"))
                    .builtin(obj.getBool("builtin", Boolean.FALSE))
                    .build());
        }
        if (list.isEmpty()) {
            return resolve(null);
        }
        return list;
    }
}
