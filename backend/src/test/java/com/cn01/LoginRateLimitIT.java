package com.cn01;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 登录失败限流：按"账号 + IP"锁定，攻击者锁不住别人；同一 IP 撞库也会被拦。 */
class LoginRateLimitIT extends IntegrationTestBase {

    private MockHttpServletRequestBuilder from(String ip, MockHttpServletRequestBuilder b) {
        return b.with(r -> { r.setRemoteAddr(ip); return r; });
    }

    @Test
    void 攻击者输错管理员密码_不影响管理员从其他IP登录() throws Exception {
        for (int i = 0; i < 6; i++) {
            call(from("10.0.0.66", json(post("/api/admin/auth/login"), Map.of("username", "admin", "password", "wrong" + i))), i < 5 ? 400 : 429);
        }
        assertThat(ok(from("10.0.0.1", json(post("/api/admin/auth/login"),
                Map.of("username", "admin", "password", "Admin12345")))).get("token").asText()).isNotBlank();
    }

    @Test
    void 同一IP轮换账号撞库_超过上限被拦() throws Exception {
        for (int i = 0; i < 20; i++) {
            call(from("10.0.0.77", json(post("/api/portal/auth/login/password"), Map.of("account", "spray" + i, "password", "x"))), 400);
        }
        assertThat(call(from("10.0.0.77", json(post("/api/portal/auth/login/password"),
                Map.of("account", "spray-next", "password", "x"))), 429).get("code").asInt()).isEqualTo(429);
        // 后台登录与前台共用同一 IP 失败计数
        call(from("10.0.0.77", json(post("/api/admin/auth/login"), Map.of("username", "admin", "password", "x"))), 429);
    }
}
