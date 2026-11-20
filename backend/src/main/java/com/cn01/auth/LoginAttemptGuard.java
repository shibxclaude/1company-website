package com.cn01.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 密码登录失败限流（后台、前台共用）：
 *   同一账号 + 同一 IP 连续失败 5 次，锁 15 分钟——只按账号锁的话，任何人输错 5 次就能把 admin 锁在门外；
 *   同一 IP 不论账号累计失败 20 次，锁 15 分钟——防止轮换账号撞库。
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {
    static final int MAX_FAILS_PER_ACCOUNT_IP = 5;
    static final int MAX_FAILS_PER_IP = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final RateLimiter rateLimiter;

    public void check(String scope, String account, String ip) {
        rateLimiter.check(ipKey(ip), MAX_FAILS_PER_IP, "登录失败次数过多，请 15 分钟后再试");
        rateLimiter.check(accountKey(scope, account, ip), MAX_FAILS_PER_ACCOUNT_IP, "密码错误次数过多，请 15 分钟后再试");
    }

    public void failed(String scope, String account, String ip) {
        rateLimiter.incr(ipKey(ip), WINDOW);
        rateLimiter.incr(accountKey(scope, account, ip), WINDOW);
    }

    public void succeeded(String scope, String account, String ip) {
        rateLimiter.reset(accountKey(scope, account, ip));
    }

    private static String accountKey(String scope, String account, String ip) {
        return "login-fail:" + scope + ":" + account + ":" + ip;
    }

    private static String ipKey(String ip) {
        return "login-fail-ip:" + ip;
    }
}
