package com.audit.platform.domain.ruleset.factory;

import com.audit.platform.domain.ruleset.RuleSet;

public interface RuleSetFactory {
    /**
     * 按用户填写字段构建新规则集。
     *
     * @param name        名称
     * @param description 说明
     * @param sceneNum    场景编号
     * @return 未持久化的规则集
     */
    RuleSet create(String name, String description, String sceneNum);

    /**
     * 按业务编码加载已有规则集。
     *
     * @param num 规则集编号
     * @return 规则集
     */
    RuleSet createByNum(String num);
}
