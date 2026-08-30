package com.audit.platform.infra.evaluation.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.audit.platform.domain.evaluation.valueobject.AttachmentVO;
import com.audit.platform.domain.evaluation.valueobject.RuleItemSnapshotVO;
import com.audit.platform.domain.evaluation.valueobject.RuleScoreVO;
import com.audit.platform.domain.evaluation.valueobject.ScoreContextVO;
import com.audit.platform.domain.scene.valueobject.SceneParamCatalog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 普通规则器：对场景参数做判空、长度、正则等校验。
 */
@Component
public class OrdinaryRuleEngine {

    public RuleScoreVO score(RuleItemSnapshotVO rule, ScoreContextVO context) {
        JSONObject config = JSONUtil.parseObj(StrUtil.blankToDefault(rule.getEngineConfigJson(), "{}"));
        JSONArray checks = config.getJSONArray("checks");
        if (checks != null) {
            for (Object item : checks) {
                JSONObject check = JSONUtil.parseObj(item);
                String fail = evaluate(check, context);
                if (fail != null) {
                    return RuleScoreVO.builder()
                            .ruleNum(rule.getRuleNum())
                            .score(BigDecimal.ZERO)
                            .rationale(fail)
                            .failed(Boolean.TRUE)
                            .failReason(fail)
                            .evidenceJson("{\"engine\":\"ordinary\"}")
                            .build();
                }
            }
        }
        return RuleScoreVO.builder()
                .ruleNum(rule.getRuleNum())
                .score(rule.getPassScore())
                .rationale("普通规则器校验通过")
                .failed(Boolean.FALSE)
                .evidenceJson("{\"engine\":\"ordinary\"}")
                .build();
    }

    private String evaluate(JSONObject check, ScoreContextVO context) {
        String paramKey = check.getStr("paramKey");
        String op = check.getStr("op");
        String expected = check.getStr("value");
        ParamValue value = resolve(paramKey, context);
        if ("NOT_BLANK".equals(op)) {
            return value.blank() ? paramKey + " 不能为空" : null;
        }
        if ("MIN_LENGTH".equals(op)) {
            int min = Integer.parseInt(expected);
            return value.length() < min ? paramKey + " 长度不能小于 " + min : null;
        }
        if ("MAX_LENGTH".equals(op)) {
            int max = Integer.parseInt(expected);
            return value.length() > max ? paramKey + " 长度不能大于 " + max : null;
        }
        if ("REGEX".equals(op)) {
            if (value.kind != Kind.TEXT) {
                return paramKey + " 不支持正则校验";
            }
            return ReUtil.isMatch(expected, StrUtil.nullToEmpty(value.text)) ? null : paramKey + " 格式不匹配";
        }
        return "不支持的校验操作：" + op;
    }

    private ParamValue resolve(String paramKey, ScoreContextVO context) {
        if (SceneParamCatalog.INPUT.equals(paramKey)) {
            return ParamValue.text(context == null ? null : context.getInputText());
        }
        if (SceneParamCatalog.ATTACHMENT.equals(paramKey)) {
            List<AttachmentVO> attachments = context == null ? List.of() : context.getAttachments();
            List<Object> list = new ArrayList<>();
            if (CollUtil.isNotEmpty(attachments)) {
                for (AttachmentVO attachment : attachments) {
                    JSONObject obj = new JSONObject();
                    obj.set("id", StrUtil.blankToDefault(attachment.getNum(), attachment.getFileName()));
                    obj.set("name", attachment.getFileName());
                    obj.set("url", StrUtil.blankToDefault(attachment.getFileUrl(), attachment.getObjectKey()));
                    list.add(obj);
                }
            }
            return ParamValue.array(list);
        }
        JSONObject extras = JSONUtil.parseObj(context == null ? "{}" : StrUtil.blankToDefault(context.getExtraParamsJson(), "{}"));
        return ParamValue.text(extras.getStr(paramKey));
    }

    private enum Kind {
        TEXT, ARRAY
    }

    private record ParamValue(Kind kind, String text, List<Object> items) {
        static ParamValue text(String value) {
            return new ParamValue(Kind.TEXT, value, List.of());
        }

        static ParamValue array(List<Object> items) {
            return new ParamValue(Kind.ARRAY, null, items == null ? List.of() : items);
        }

        boolean blank() {
            if (kind == Kind.ARRAY) {
                return CollUtil.isEmpty(items);
            }
            return StrUtil.isBlank(text);
        }

        int length() {
            if (kind == Kind.ARRAY) {
                return items == null ? 0 : items.size();
            }
            return StrUtil.length(text);
        }
    }
}
