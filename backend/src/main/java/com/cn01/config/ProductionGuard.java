package com.cn01.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境启动校验：APP_ENV=prod（systemd 单元里已固定设置）时，只要还开着模拟短信、模拟支付或 JWT 密钥不合格，
 * 直接拒绝启动——漏改一项不能"静默上线"。开发、测试环境不受影响。
 */
@Slf4j
@Component
public class ProductionGuard implements BeanFactoryPostProcessor, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /** 在任何业务 Bean（数据库、Redis 连接）创建之前执行，确保报的是这里的错误而不是被连接失败盖住 */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        AppProperties props = Binder.get(environment).bind("app", AppProperties.class).orElseGet(AppProperties::new);
        enforce(environment.getProperty("app.env", "dev"), props);
    }

    public static List<String> violations(AppProperties p) {
        List<String> v = new ArrayList<>();
        if (p.getSms().isMock()) v.add("SMS_MOCK=true：验证码会在接口中返回，任何人都能冒用他人手机号登录");
        if (p.getPay().isMock()) v.add("PAY_MODE=mock：任何人都能调用模拟支付接口把订单改成已支付");
        if (weakSecret(p.getJwt().getSecret())) v.add("JWT_SECRET 未配置、不足 32 字节或仍是示例占位符");
        return v;
    }

    public static void enforce(String env, AppProperties p) {
        if (!"prod".equalsIgnoreCase(env)) return;
        List<String> v = violations(p);
        if (v.isEmpty()) return;
        String msg = "生产环境配置不安全，拒绝启动：\n  - " + String.join("\n  - ", v) + "\n请修改 /etc/cn01/backend.env";
        log.error(msg);
        throw new IllegalStateException(msg);
    }

    private static boolean weakSecret(String s) {
        return s == null || s.getBytes(StandardCharsets.UTF_8).length < 32
                || !s.chars().allMatch(c -> c < 128) || s.contains("openssl");
    }
}
