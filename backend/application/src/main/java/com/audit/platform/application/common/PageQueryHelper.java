package com.audit.platform.application.common;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * 列表检索：编号、名称分别模糊匹配；仅填 keyword 时二者取或。
 */
public final class PageQueryHelper {
    private PageQueryHelper() {
    }

    public static <T> void likeNumAndName(LambdaQueryWrapper<T> wrapper, String num, String name, String keyword,
                                          SFunction<T, String> numColumn, SFunction<T, String> nameColumn) {
        boolean hasNum = StrUtil.isNotBlank(num);
        boolean hasName = StrUtil.isNotBlank(name);
        if (hasNum) {
            wrapper.like(numColumn, num.trim());
        }
        if (hasName) {
            wrapper.like(nameColumn, name.trim());
        }
        if (!hasNum && !hasName && StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(numColumn, kw).or().like(nameColumn, kw));
        }
    }
}
