package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class SolutionIT extends IntegrationTestBase {

    @Test
    void 访客无需登录即可看到方案列表_价格_价格公示() throws Exception {
        JsonNode list = ok(get("/api/portal/solutions"));
        assertThat(list).hasSize(6);
        list.forEach(c -> assertThat(new BigDecimal(c.get("minPrice").asText())).isPositive());

        JsonNode detail = ok(get("/api/portal/solutions/data-fusion"));
        assertThat(detail.get("name").asText()).isEqualTo("多维数据融合应用平台");
        assertThat(detail.get("versions")).hasSize(2);
        assertThat(detail.get("services")).hasSize(3);
        assertThat(detail.get("deliveryLabel").asText()).isEqualTo("远程部署");

        JsonNode price = ok(get("/api/portal/price"));
        assertThat(price.get("solutions")).hasSize(6);

        assertThat(ok(get("/api/portal/solutions").param("category", "platform"))).hasSize(2);
        assertThat(ok(get("/api/portal/solutions").param("keyword", "物联网"))).isNotEmpty();
        assertThat(ok(get("/api/portal/solutions").param("recommended", "true"))).hasSize(3);
        assertThat(call(get("/api/portal/solutions/not-exist"), 404).get("code").asInt()).isEqualTo(404);
    }

    @Test
    void 后台接口必须登录() throws Exception {
        JsonNode r = call(get("/api/admin/solutions"), 401);
        assertThat(r.get("code").asInt()).isEqualTo(401);
        assertThat(call(auth(get("/api/admin/solutions"), "bad-token"), 401).get("msg").asText()).isEqualTo("请先登录");
    }

    @Test
    void 前台用户token不能访问后台() throws Exception {
        String phone = "13800000011";
        String code = ok(json(post("/api/portal/sms/code"), Map.of("phone", phone, "scene", "LOGIN"))).get("mockCode").asText();
        String userToken = ok(json(post("/api/portal/auth/login/sms"), Map.of("phone", phone, "code", code))).get("token").asText();
        call(auth(get("/api/admin/solutions"), userToken), 401);
    }

    @Test
    void 后台改价后前台立即生效_缓存被清除() throws Exception {
        String token = adminToken();
        // 先读一次，让前台缓存生效
        BigDecimal before = new BigDecimal(ok(get("/api/portal/solutions/iot")).get("versions").get(0).get("price").asText());
        assertThat(redis.hasKey("cn01:portal:solution::iot")).isTrue();

        long id = ok(get("/api/portal/solutions/iot")).get("id").asLong();
        ObjectNode req = editable(ok(auth(get("/api/admin/solutions/" + id), token)));
        BigDecimal newPrice = before.add(new BigDecimal("1000.50"));
        ((ObjectNode) req.get("versions").get(0)).put("price", newPrice);
        ok(auth(json(put("/api/admin/solutions/" + id), req), token));

        JsonNode after = ok(get("/api/portal/solutions/iot"));
        assertThat(new BigDecimal(after.get("versions").get(0).get("price").asText())).isEqualByComparingTo(newPrice);
        JsonNode card = findCard(ok(get("/api/portal/solutions")), "iot");
        assertThat(new BigDecimal(card.get("minPrice").asText())).isEqualByComparingTo(newPrice);
    }

    @Test
    void 新增方案_下架后前台不可见_富文本被清洗() throws Exception {
        String token = adminToken();
        ObjectNode req = editable(ok(auth(get("/api/admin/solutions/1"), token)));
        req.put("slug", "new-solution-x");
        req.put("name", "新方案X");
        req.put("content", "<p onclick=\"alert(1)\">介绍<script>alert(1)</script></p><img src=\"/uploads/202611/a.png\">");
        ((ArrayNode) req.get("versions")).forEach(v -> ((ObjectNode) v).remove("id"));
        ((ArrayNode) req.get("services")).forEach(v -> ((ObjectNode) v).remove("id"));
        JsonNode created = ok(auth(json(post("/api/admin/solutions"), req), token));
        String content = created.get("content").asText();
        assertThat(content).doesNotContain("script").doesNotContain("onclick").contains("<img src=\"/uploads/202611/a.png\">");

        assertThat(ok(get("/api/portal/solutions/new-solution-x")).get("name").asText()).isEqualTo("新方案X");
        ok(auth(json(put("/api/admin/solutions/" + created.get("id").asLong() + "/status"), Map.of("status", 0)), token));
        call(get("/api/portal/solutions/new-solution-x"), 404);

        // 删除后 slug 释放，可以再次使用
        ok(auth(delete("/api/admin/solutions/" + created.get("id").asLong()), token));
        assertThat(ok(auth(json(post("/api/admin/solutions"), req), token)).get("slug").asText()).isEqualTo("new-solution-x");
    }

    @Test
    void 参数校验返回统一结构() throws Exception {
        String token = adminToken();
        ObjectNode req = editable(ok(auth(get("/api/admin/solutions/1"), token)));
        req.put("slug", "Bad Slug!");
        ((ObjectNode) req.get("versions").get(0)).put("price", -1);
        JsonNode r = call(auth(json(put("/api/admin/solutions/1"), req), token), 400);
        assertThat(r.get("code").asInt()).isEqualTo(400);
        assertThat(r.get("msg").asText()).contains("URL 标识").contains("价格必须大于 0");
        assertThat(r.has("data")).isFalse();

        // 上架状态但没有在售版本
        ObjectNode req2 = editable(ok(auth(get("/api/admin/solutions/1"), token)));
        ((ArrayNode) req2.get("versions")).forEach(v -> ((ObjectNode) v).put("status", 0));
        assertThat(call(auth(json(put("/api/admin/solutions/1"), req2), token), 400).get("msg").asText()).contains("至少需要一个在售版本");
    }

    @Test
    void 分类下有方案时不能删除() throws Exception {
        String token = adminToken();
        assertThat(call(auth(delete("/api/admin/categories/1"), token), 409).get("msg").asText()).contains("该分类下还有");
    }

    /** 后台详情 -> 编辑请求体 */
    private ObjectNode editable(JsonNode detail) {
        ObjectNode r = om.createObjectNode();
        for (String f : new String[]{"categoryId", "slug", "name", "subtitle", "summary", "mainImage", "gallery", "content",
                "deliveryMode", "afterSales", "servicePeriod", "recommended", "status", "sort"}) {
            if (detail.has(f)) r.set(f, detail.get(f));
        }
        ArrayNode versions = r.putArray("versions");
        detail.get("versions").forEach(v -> {
            ObjectNode n = versions.addObject();
            for (String f : new String[]{"id", "name", "price", "originalPrice", "billingType", "description", "status", "sort"})
                if (v.has(f)) n.set(f, v.get(f));
        });
        ArrayNode services = r.putArray("services");
        detail.get("services").forEach(v -> {
            ObjectNode n = services.addObject();
            for (String f : new String[]{"id", "name", "price", "unit", "description", "status", "sort"})
                if (v.has(f)) n.set(f, v.get(f));
        });
        return r;
    }

    private static JsonNode findCard(JsonNode list, String slug) {
        for (JsonNode c : list) if (c.get("slug").asText().equals(slug)) return c;
        throw new AssertionError("找不到 " + slug);
    }
}
