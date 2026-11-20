package com.cn01.auth;

import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** 基于 Redis 计数的固定窗口限流：登录失败、短信发送、下单等。 */
@Component
@RequiredArgsConstructor
public class RateLimiter {
    private final StringRedisTemplate redis;

    /**
     * INCR 与 EXPIRE 放在一个 Lua 脚本里原子执行；并且只要 key 没有过期时间就补上——
     * 分两条命令时，INCR 成功而 EXPIRE 没执行的 key 会永不过期，对应的手机号/IP 被永久限流。
     */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local n = redis.call('INCR', KEYS[1])
            if redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return n
            """, Long.class);

    /** 计数 +1，超过 limit 抛 429。 */
    public void hit(String key, int limit, Duration window, String msg) {
        long n = increment(key, window);
        if (n > limit) throw new BizException(ErrorCode.TOO_MANY_REQUESTS, msg);
    }

    private long increment(String key, Duration window) {
        Long n = redis.execute(INCR_WITH_TTL, List.of("cn01:rl:" + key), String.valueOf(Math.max(1, window.toSeconds())));
        return n == null ? 0 : n;
    }

    /** 只检查不计数 */
    public void check(String key, int limit, String msg) {
        String v = redis.opsForValue().get("cn01:rl:" + key);
        if (v != null && Long.parseLong(v) >= limit) throw new BizException(ErrorCode.TOO_MANY_REQUESTS, msg);
    }

    public void incr(String key, Duration window) {
        increment(key, window);
    }

    public void reset(String key) {
        redis.delete("cn01:rl:" + key);
    }
}
