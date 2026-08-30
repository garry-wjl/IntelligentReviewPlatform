package com.audit.platform.adapter.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.audit.platform.application.access.AccessCommandService;
import com.audit.platform.client.access.dto.ApiKeyAuthParamDTO;
import com.audit.platform.client.access.dto.CredentialAuthDTO;
import com.audit.platform.facade.common.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 开放 API Key 校验。
 */
@Component
@Order(2)
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    @Resource
    private AccessCommandService accessCommandService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/open/v1");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String secret = request.getHeader("X-Api-Key");
        if (StrUtil.isBlank(secret)) {
            String auth = request.getHeader("Authorization");
            if (StrUtil.startWithIgnoreCase(auth, "Bearer ")) {
                secret = StrUtil.trim(auth.substring(7));
            }
        }
        if (StrUtil.isBlank(secret)) {
            writeUnauthorized(response, "缺少 API Key");
            return;
        }
        try {
            ApiKeyAuthParamDTO param = new ApiKeyAuthParamDTO();
            param.setRawSecret(secret);
            param.setOperatorId("open-api");
            CredentialAuthDTO auth = accessCommandService.authenticate(param);
            request.setAttribute(BaseController.ATTR_USER_ID, auth.getCredentialNum());
            request.setAttribute(BaseController.ATTR_CREDENTIAL_NUM, auth.getCredentialNum());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, e.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(Result.fail(401, msg)));
    }
}
