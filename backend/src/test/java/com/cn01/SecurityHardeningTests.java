package com.cn01;

import com.cn01.config.AppProperties;
import com.cn01.config.CacheConfig;
import com.cn01.config.ProductionGuard;
import com.cn01.news.NewsDtos;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** 上线安全加固：生产环境启动校验、缓存反序列化白名单。 */
class SecurityHardeningTests {

    private static AppProperties safeProps() {
        AppProperties p = new AppProperties();
        p.getJwt().setSecret("0123456789abcdef0123456789abcdef-strong");
        p.getSms().setMock(false);
        p.getPay().setMode("wechat");
        p.getShop().setEnabled(true);
        return p;
    }

    @Test
    void 生产校验_安全配置无违规() {
        assertThat(ProductionGuard.violations(safeProps())).isEmpty();
    }

    @Test
    void 生产校验_模拟短信_模拟支付_弱密钥都被列出() {
        AppProperties p = safeProps();
        p.getSms().setMock(true);
        p.getPay().setMode("mock");
        p.getJwt().setSecret("short");
        assertThat(ProductionGuard.violations(p)).hasSize(3)
                .anyMatch(s -> s.contains("SMS_MOCK"))
                .anyMatch(s -> s.contains("PAY_MODE"))
                .anyMatch(s -> s.contains("JWT_SECRET"));
    }

    @Test
    void 生产校验_示例文件里的占位密钥视为未配置() {
        AppProperties p = safeProps();
        p.getJwt().setSecret("请用openssl生成请用openssl生成请用openssl生成");
        assertThat(ProductionGuard.violations(p)).anyMatch(s -> s.contains("JWT_SECRET"));
    }

    @Test
    void 生产校验_购买未开放时不要求真实短信和支付_但仍校验JWT() {
        AppProperties p = new AppProperties();   // 默认：购买关闭、模拟短信、模拟支付
        p.getJwt().setSecret("0123456789abcdef0123456789abcdef-strong");
        assertThat(p.getShop().isEnabled()).isFalse();
        assertThat(ProductionGuard.violations(p)).isEmpty();
        p.getJwt().setSecret("short");
        assertThat(ProductionGuard.violations(p)).hasSize(1).allMatch(s -> s.contains("JWT_SECRET"));
    }

    @Test
    void 生产校验_只有prod环境才拒绝启动() {
        AppProperties bad = new AppProperties();
        bad.getShop().setEnabled(true);
        assertThatCode(() -> ProductionGuard.enforce("dev", bad)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProductionGuard.enforce("prod", bad))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SMS_MOCK");
        assertThatCode(() -> ProductionGuard.enforce("prod", safeProps())).doesNotThrowAnyException();
    }

    @Test
    void 缓存序列化_业务类型可往返() {
        RedisSerializer<Object> ser = CacheConfig.valueSerializer();
        var list = new ArrayList<>(List.of(new NewsDtos.NewsItem(1L, "标题", "摘要", "/c.png",
                LocalDateTime.of(2026, 1, 2, 3, 4, 5), 0, 1, 7)));
        assertThat(ser.deserialize(ser.serialize(list))).isEqualTo(list);
    }

    @Test
    void 缓存序列化_拒绝白名单外的类型() {
        RedisSerializer<Object> ser = CacheConfig.valueSerializer();
        byte[] gadget = "[\"java.io.File\",\"/etc/passwd\"]".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> ser.deserialize(gadget)).isInstanceOf(Exception.class);
    }
}
