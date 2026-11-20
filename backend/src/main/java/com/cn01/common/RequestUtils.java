package com.cn01.common;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {
    private RequestUtils() {}

    /** server.forward-headers-strategy=native 时 getRemoteAddr 已是 nginx 传来的真实 IP */
    public static String clientIp(HttpServletRequest req) {
        return req.getRemoteAddr();
    }
}
