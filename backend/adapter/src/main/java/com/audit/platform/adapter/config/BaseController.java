package com.audit.platform.adapter.config;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

public class BaseController {
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_CREDENTIAL_NUM = "credentialNum";

    @Resource
    protected HttpServletRequest request;

    protected String getCurrentUserId() {
        Object value = request.getAttribute(ATTR_USER_ID);
        return value == null ? "system" : String.valueOf(value);
    }

    protected String getCredentialNum() {
        Object value = request.getAttribute(ATTR_CREDENTIAL_NUM);
        return value == null ? null : String.valueOf(value);
    }

    protected boolean isLogin() {
        return request.getAttribute(ATTR_USER_ID) != null;
    }

    protected void fillOperator(Object param) {
        if (param instanceof com.audit.platform.client.common.dto.OperatorParamDTO operator) {
            operator.setOperatorId(getCurrentUserId());
        } else {
            try {
                param.getClass().getMethod("setOperatorId", String.class).invoke(param, getCurrentUserId());
            } catch (Exception ignored) {
                // 无 operatorId 字段时跳过
            }
        }
    }
}
