package com.cn01.sms;

import com.cn01.auth.RateLimiter;
import com.cn01.common.BizException;
import com.cn01.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

/**
 * 短信验证码：6 位数字，5 分钟有效，校验错误 5 次作废；
 * 同一手机号 60 秒 1 条、每天 10 条；同一 IP 每小时 20 条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {
    public enum Scene { LOGIN, REGISTER }

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final RateLimiter rateLimiter;
    private final SmsSender sender;
    private final AppProperties props;

    @PostConstruct
    void checkChannel() {
        if (sender instanceof LogSmsSender) {
            if (props.getSms().isMock()) log.warn("短信为模拟模式：验证码只写日志并在接口中返回，仅限开发演示，上线前必须关闭（SMS_MOCK=false）并接入短信服务");
            else log.error("SMS_MOCK=false 但尚未接入短信服务（当前只有日志实现 LogSmsSender），用户将收不到验证码！请实现 SmsSender 并注册为 @Primary Bean");
        }
    }

    /** @return 开发模式（app.sms.mock=true）下返回验证码，便于演示；生产返回 null */
    public String send(String phone, Scene scene, String ip) {
        rateLimiter.hit("sms-interval:" + phone, 1, Duration.ofSeconds(60), "发送太频繁，请 60 秒后再试");
        rateLimiter.hit("sms-day:" + phone, 10, Duration.ofDays(1), "该手机号今日验证码次数已用完");
        rateLimiter.hit("sms-ip:" + ip, 20, Duration.ofHours(1), "发送太频繁，请稍后再试");
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(key(phone, scene), code, TTL);
        redis.delete(failKey(phone, scene));
        sender.sendCode(phone, code);
        return props.getSms().isMock() ? code : null;
    }

    /**
     * 比对与消费在一个 Lua 脚本里原子完成：分成"读出比对 → 删除"两步时，同一个验证码并发提交可以通过多次。
     * 返回 1 通过；-1 已过期；0 不正确；-2 错误次数达到上限（验证码作废）。
     */
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            local v = redis.call('GET', KEYS[1])
            if not v then return -1 end
            if v == ARGV[1] then
              redis.call('DEL', KEYS[1], KEYS[2])
              return 1
            end
            local f = redis.call('INCR', KEYS[2])
            redis.call('EXPIRE', KEYS[2], ARGV[2])
            if f >= tonumber(ARGV[3]) then
              redis.call('DEL', KEYS[1])
              return -2
            end
            return 0
            """, Long.class);
    private static final int MAX_FAILS = 5;

    /** 校验并消费验证码 */
    public void verify(String phone, Scene scene, String code) {
        Long r = redis.execute(VERIFY, List.of(key(phone, scene), failKey(phone, scene)),
                code == null ? "" : code, String.valueOf(TTL.toSeconds()), String.valueOf(MAX_FAILS));
        if (r == null || r == -1) throw BizException.badRequest("验证码已过期，请重新获取");
        if (r == -2) throw BizException.badRequest("验证码错误次数过多，请重新获取");
        if (r == 0) throw BizException.badRequest("验证码不正确");
    }

    private static String key(String phone, Scene scene) {
        return "cn01:sms:" + scene + ":" + phone;
    }

    private static String failKey(String phone, Scene scene) {
        return "cn01:sms-fail:" + scene + ":" + phone;
    }
}
