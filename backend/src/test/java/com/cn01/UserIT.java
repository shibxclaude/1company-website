package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class UserIT extends IntegrationTestBase {

    private String code(String phone, String scene) throws Exception {
        return ok(json(post("/api/portal/sms/code"), Map.of("phone", phone, "scene", scene))).get("mockCode").asText();
    }

    @Test
    void 注册_密码登录_我的订单包含同手机号的游客订单() throws Exception {
        String phone = "13700000001";
        // 先以游客身份下单
        JsonNode d = ok(get("/api/portal/solutions/iot"));
        JsonNode created = ok(json(post("/api/portal/orders"), Map.of("slug", "iot",
                "versionId", d.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "李四", "contactPhone", phone, "contactEmail", "lisi@example.com", "agreed", true)));

        JsonNode reg = ok(json(post("/api/portal/auth/register"),
                Map.of("phone", phone, "code", code(phone, "REGISTER"), "password", "abc12345", "username", "lisi01")));
        assertThat(reg.get("user").get("phone").asText()).isEqualTo(phone);

        // 重复注册：拿到有效验证码后仍提示已注册（先清掉 60 秒发送间隔限制）
        redis.delete("cn01:rl:sms-interval:" + phone);
        assertThat(call(json(post("/api/portal/auth/register"),
                Map.of("phone", phone, "code", code(phone, "REGISTER"), "password", "abc12345")), 409)
                .get("msg").asText()).contains("已注册");

        String token = ok(json(post("/api/portal/auth/login/password"), Map.of("account", "lisi01", "password", "abc12345"))).get("token").asText();
        JsonNode mine = ok(auth(get("/api/user/orders"), token));
        assertThat(mine.get("list").findValuesAsText("orderNo")).contains(created.get("orderNo").asText());
        // 登录后不带 token 也能查看自己的订单详情
        ok(auth(get("/api/portal/orders/" + created.get("orderNo").asText()), token));

        assertThat(ok(auth(get("/api/user/me"), token)).get("hasPassword").asBoolean()).isTrue();
    }

    @Test
    void 密码错误与验证码错误() throws Exception {
        assertThat(call(json(post("/api/portal/auth/login/password"), Map.of("account", "nobody", "password", "x")), 400)
                .get("msg").asText()).isEqualTo("账号或密码错误");
        String phone = "13700000002";
        code(phone, "LOGIN");
        assertThat(call(json(post("/api/portal/auth/login/sms"), Map.of("phone", phone, "code", "000000")), 400)
                .get("msg").asText()).contains("验证码");
        // 60 秒内重复发送被限流
        assertThat(call(json(post("/api/portal/sms/code"), Map.of("phone", phone, "scene", "LOGIN")), 429).get("code").asInt()).isEqualTo(429);
    }

    @Test
    void 验证码登录自动注册_用户中心需要登录() throws Exception {
        call(get("/api/user/orders"), 401);
        String phone = "13700000003";
        JsonNode r = ok(json(post("/api/portal/auth/login/sms"), Map.of("phone", phone, "code", code(phone, "LOGIN"))));
        assertThat(r.get("user").get("hasPassword").asBoolean()).isFalse();
        ok(auth(get("/api/user/orders"), r.get("token").asText()));
    }

    @Test
    void 后台停用用户后token立即失效() throws Exception {
        String phone = "13700000004";
        JsonNode r = ok(json(post("/api/portal/auth/login/sms"), Map.of("phone", phone, "code", code(phone, "LOGIN"))));
        String admin = adminToken();
        ok(auth(json(put("/api/admin/users/" + r.get("user").get("id").asLong() + "/status"), Map.of("status", 0)), admin));
        call(auth(get("/api/user/orders"), r.get("token").asText()), 401);
    }
}
