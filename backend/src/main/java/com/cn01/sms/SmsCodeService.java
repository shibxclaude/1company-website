package com.cn01.sms;

import com.cn01.auth.RateLimiter;
import com.cn01.common.BizException;
import com.cn01.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

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

    /** 校验并消费验证码 */
    public void verify(String phone, Scene scene, String code) {
        String expected = redis.opsForValue().get(key(phone, scene));
        if (expected == null) throw BizException.badRequest("验证码已过期，请重新获取");
        if (!expected.equals(code)) {
            Long fails = redis.opsForValue().increment(failKey(phone, scene));
            redis.expire(failKey(phone, scene), TTL);
            if (fails != null && fails >= 5) {
                redis.delete(key(phone, scene));
                throw BizException.badRequest("验证码错误次数过多，请重新获取");
            }
            throw BizException.badRequest("验证码不正确");
        }
        redis.delete(key(phone, scene));
        redis.delete(failKey(phone, scene));
    }

    private static String key(String phone, Scene scene) {
        return "cn01:sms:" + scene + ":" + phone;
    }

    private static String failKey(String phone, Scene scene) {
        return "cn01:sms-fail:" + scene + ":" + phone;
    }
}
