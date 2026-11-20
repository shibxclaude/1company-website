package com.cn01;

import com.cn01.auth.RateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 改密后旧 token 失效、停用账号的 token 在前台也不再生效、订单凭证走请求头、限流计数一定带过期时间。 */
class AuthHardeningIT extends IntegrationTestBase {
    @Autowired RateLimiter rateLimiter;

    private String smsLogin(String phone) throws Exception {
        String code = ok(json(post("/api/portal/sms/code"), Map.of("phone", phone, "scene", "LOGIN"))).get("mockCode").asText();
        return ok(json(post("/api/portal/auth/login/sms"), Map.of("phone", phone, "code", code))).get("token").asText();
    }

    @Test
    void 前台改密码_旧token失效_返回的新token可用() throws Exception {
        String old = smsLogin("13600000001");
        JsonNode r = ok(json(auth(put("/api/user/password"), old), Map.of("newPassword", "abc12345")));
        String fresh = r.get("token").asText();
        call(auth(get("/api/user/me"), old), 401);
        ok(auth(get("/api/user/me"), fresh));
    }

    @Test
    void 后台改密码_旧token失效_返回的新token可用() throws Exception {
        String old = adminToken();
        String fresh = ok(json(auth(put("/api/admin/auth/password"), old),
                Map.of("oldPassword", "Admin12345", "newPassword", "Admin12345x"))).get("token").asText();
        try {
            call(auth(get("/api/admin/dashboard"), old), 401);
            ok(auth(get("/api/admin/dashboard"), fresh));
        } finally {
            // 其他测试依赖初始密码，改回去
            ok(json(auth(put("/api/admin/auth/password"), fresh), Map.of("oldPassword", "Admin12345x", "newPassword", "Admin12345")));
        }
    }

    @Test
    void 停用用户的token在前台接口按游客处理() throws Exception {
        String phone = "13600000002";
        String token = smsLogin(phone);
        long uid = ok(auth(get("/api/user/me"), token)).get("id").asLong();
        ok(json(auth(put("/api/admin/users/" + uid + "/status"), adminToken()), Map.of("status", 0)));

        JsonNode d = ok(get("/api/portal/solutions/iot"));
        JsonNode created = ok(json(auth(post("/api/portal/orders"), token), Map.of("slug", "iot",
                "versionId", d.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "王五", "contactPhone", "13600000099", "contactEmail", "w@example.com", "agreed", true)));
        JsonNode detail = ok(auth(get("/api/admin/orders/" + created.get("orderNo").asText()), adminToken()));
        assertThat(detail.get("userId")).isNull();
    }

    @Test
    void 订单凭证可以放在请求头里() throws Exception {
        JsonNode d = ok(get("/api/portal/solutions/iot"));
        JsonNode created = ok(json(post("/api/portal/orders"), Map.of("slug", "iot",
                "versionId", d.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "赵六", "contactPhone", "13600000003", "contactEmail", "z@example.com", "agreed", true)));
        String no = created.get("orderNo").asText();
        ok(get("/api/portal/orders/" + no).header("X-Order-Token", created.get("accessToken").asText()));
        call(get("/api/portal/orders/" + no).header("X-Order-Token", "wrong"), 404);
    }

    @Test
    void 限流计数_遗留的无过期key也会补上过期时间() {
        redis.opsForValue().set("cn01:rl:stuck-key", "3");   // 模拟 INCR 成功但 EXPIRE 没执行
        rateLimiter.hit("stuck-key", 100, Duration.ofMinutes(5), "x");
        assertThat(redis.getExpire("cn01:rl:stuck-key")).isPositive();
        rateLimiter.incr("stuck-key2", Duration.ofMinutes(5));
        assertThat(redis.getExpire("cn01:rl:stuck-key2")).isPositive();
    }
}
