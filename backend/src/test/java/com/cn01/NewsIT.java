package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class NewsIT extends IntegrationTestBase {
    @Test
    void 种子新闻立即可见_时区一致() throws Exception {
        JsonNode page = ok(get("/api/portal/news"));
        assertThat(page.get("total").asInt()).isGreaterThanOrEqualTo(1);
        long id = page.get("list").get(0).get("id").asLong();
        assertThat(ok(get("/api/portal/news/" + id)).get("content").asText()).contains("官网");
    }

    @Test
    void 后台发布_定时发布_草稿不可见() throws Exception {
        String admin = adminToken();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> req = new HashMap<>(Map.of("title", "定时新闻", "content", "<p>正文<script>x</script></p>", "top", 0, "status", 1,
                "publishedAt", LocalDateTime.now().plusDays(1).format(f)));
        long future = ok(auth(json(post("/api/admin/news"), req), admin)).get("id").asLong();
        call(get("/api/portal/news/" + future), 404);

        req.put("publishedAt", LocalDateTime.now().minusMinutes(1).format(f));
        JsonNode updated = ok(auth(json(put("/api/admin/news/" + future), req), admin));
        assertThat(updated.get("content").asText()).doesNotContain("script");
        assertThat(ok(get("/api/portal/news/" + future)).get("title").asText()).isEqualTo("定时新闻");

        ok(auth(json(put("/api/admin/news/" + future + "/status"), Map.of("status", 0)), admin));
        call(get("/api/portal/news/" + future), 404);
    }
}
