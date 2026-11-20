package com.cn01.site;

import com.cn01.common.ErrorCode;
import com.cn01.common.Result;
import com.cn01.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 在线购买与会员服务开关（app.shop.enabled）。关闭时下单、支付、短信、注册登录、用户中心接口一律返回 403"暂未开放"，
 * 支付与短信接入之前不会有人能用模拟通道冒用手机号登录或把订单标成已支付。方案展示、价格公示、管理后台不受影响。
 */
@Component
@RequiredArgsConstructor
public class ShopGate implements HandlerInterceptor {
    /** 由 WebMvcConfig 注册拦截 */
    public static final List<String> PATHS = List.of(
            "/api/portal/orders", "/api/portal/orders/**",
            "/api/portal/sms/**", "/api/portal/auth/**",
            "/api/user/**", "/api/pay/mock/**");

    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public String closedMessage() {
        return "在线购买与会员服务暂未开放，请致电 " + props.getShop().getServicePhone() + " 咨询";
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if (props.getShop().isEnabled() || "OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(ErrorCode.FORBIDDEN, closedMessage())));
        return false;
    }
}
