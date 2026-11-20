package com.cn01.auth;

import com.cn01.admin.AdminUserMapper;
import com.cn01.common.Status;
import com.cn01.user.AppUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 解析登录身份（所有 /api/** 接口）：token 有效则写入 AuthContext，没有或无效则当作未登录。
 * 这里只解析、不拦截；"哪些路径必须是谁"由 {@link RoleGuard} 按路由模式注册（见 WebMvcConfig）。
 * 不能在这里用 getRequestURI() 前缀判断：Spring 匹配路由前会解码并去掉 ;参数，
 * /api/admin;x/orders、/api/%61dmin/orders 都会路由到后台接口，却不以 "/api/admin/" 开头。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final AdminUserMapper adminUserMapper;
    private final AppUserMapper appUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        // 带了 token 就逐次核对账号：已停用、或改过密码（版本号变了）的 token 一律当作没登录。
        // 前台接口（/api/portal/**）也核对，避免停用账号继续以本人身份下单、查单。
        Optional<AuthPrincipal> principal = jwtService.parse(bearer(req)).filter(this::stillValid);
        principal.ifPresent(p -> req.setAttribute(AuthContext.ATTR, p));
        return true;
    }

    private boolean stillValid(AuthPrincipal p) {
        if (p.type() == AuthPrincipal.Type.ADMIN) {
            var u = adminUserMapper.selectById(p.id());
            return u != null && u.getStatus() == Status.ON && AuthPrincipal.ver(u.getTokenVersion()) == p.ver();
        }
        var u = appUserMapper.selectById(p.id());
        return u != null && u.getStatus() == Status.ON && AuthPrincipal.ver(u.getTokenVersion()) == p.ver();
    }

    private static String bearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }
}
