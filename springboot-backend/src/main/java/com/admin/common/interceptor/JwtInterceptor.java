package com.admin.common.interceptor;


import com.admin.common.exception.UnauthorizedException;
import com.admin.common.utils.JwtUtil;
import com.admin.entity.User;
import com.admin.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * JWT 拦截器：校验 token，并从数据库确认用户仍有效。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final int USER_STATUS_ACTIVE = 1;

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("未登录或token已过期");
        }

        if (!JwtUtil.validateToken(token)) {
            throw new UnauthorizedException("无效的token或token已过期");
        }

        Long userId = JwtUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != USER_STATUS_ACTIVE) {
            throw new UnauthorizedException("用户不存在或已禁用");
        }

        return true;
    }
}
