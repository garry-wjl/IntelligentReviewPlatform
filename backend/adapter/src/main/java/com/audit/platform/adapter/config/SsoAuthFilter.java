package com.audit.platform.adapter.config;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 管理端 SSO 适配点：开发期接受 Bearer 或 X-Operator-Id，缺省 dev-user。
 */
@Component
@Order(1)
public class SsoAuthFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin/v1");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String operator = request.getHeader("X-Operator-Id");
        if (StrUtil.isBlank(operator)) {
            String auth = request.getHeader("Authorization");
            if (StrUtil.startWithIgnoreCase(auth, "Bearer ")) {
                operator = StrUtil.trim(auth.substring(7));
            }
        }
        if (StrUtil.isBlank(operator)) {
            operator = "dev-user";
        }
        request.setAttribute(BaseController.ATTR_USER_ID, operator);
        filterChain.doFilter(request, response);
    }
}
