package com.cn01.auth;

import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 基于 Redis 计数的固定窗口限流：登录失败、短信发送、下单等。 */
@Component
@RequiredArgsConstructor
public class RateLimiter {
    private final StringRedisTemplate redis;

    /** 计数 +1，超过 limit 抛 429。 */
    public void hit(String key, int limit, Duration window, String msg) {
        Long n = redis.opsForValue().increment("cn01:rl:" + key);
        if (n != null && n == 1) redis.expire("cn01:rl:" + key, window);
        if (n != null && n > limit) throw new BizException(ErrorCode.TOO_MANY_REQUESTS, msg);
    }

    /** 只检查不计数 */
    public void check(String key, int limit, String msg) {
        String v = redis.opsForValue().get("cn01:rl:" + key);
        if (v != null && Long.parseLong(v) >= limit) throw new BizException(ErrorCode.TOO_MANY_REQUESTS, msg);
    }

    public void incr(String key, Duration window) {
        Long n = redis.opsForValue().increment("cn01:rl:" + key);
        if (n != null && n == 1) redis.expire("cn01:rl:" + key, window);
    }

    public void reset(String key) {
        redis.delete("cn01:rl:" + key);
    }
}
