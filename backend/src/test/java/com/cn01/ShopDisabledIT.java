package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 支付、短信未接入时关闭在线购买与会员服务：相关接口一律"暂未开放"，展示与后台不受影响。 */
@TestPropertySource(properties = "app.shop.enabled=false")
class ShopDisabledIT extends IntegrationTestBase {

    private void closed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        assertThat(call(req, 403).get("msg").asText()).contains("暂未开放").contains("136 9928 5133");
    }

    @Test
    void 站点配置告知前台购买未开放() throws Exception {
        JsonNode c = ok(get("/api/portal/site-config"));
        assertThat(c.get("shopEnabled").asBoolean()).isFalse();
    }

    @Test
    void 购买_短信_登录注册_用户中心_模拟支付都不可用() throws Exception {
        long versionId = ok(get("/api/portal/solutions/iot")).get("versions").get(0).get("id").asLong();
        closed(json(post("/api/portal/orders"), Map.of("slug", "iot", "versionId", versionId, "quantity", 1,
                "contactName", "x", "contactPhone", "13300000001", "contactEmail", "x@example.com", "agreed", true)));
        closed(json(post("/api/portal/sms/code"), Map.of("phone", "13300000001", "scene", "LOGIN")));
        closed(json(post("/api/portal/auth/login/password"), Map.of("account", "a", "password", "b")));
        closed(json(post("/api/portal/auth/register"), Map.of("phone", "13300000001", "code", "123456", "password", "abc12345")));
        closed(get("/api/user/me"));
        closed(get("/api/portal/orders/2026000000000000000000"));
        closed(post("/api/pay/mock/2026000000000000000000/succeed"));
    }

    @Test
    void 方案展示与管理后台照常可用() throws Exception {
        ok(get("/api/portal/solutions"));
        ok(get("/api/portal/price"));
        ok(auth(get("/api/admin/dashboard"), adminToken()));
    }
}
