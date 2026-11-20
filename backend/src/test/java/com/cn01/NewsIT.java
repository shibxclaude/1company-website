package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.news.News;
import com.cn01.news.NewsMapper;
import com.cn01.news.NewsPublishJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class NewsIT extends IntegrationTestBase {
    @Autowired NewsMapper newsMapper;
    @Autowired NewsPublishJob publishJob;

    private long createNews(String title, LocalDateTime publishedAt) throws Exception {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ok(auth(json(post("/api/admin/news"), Map.of("title", title, "content", "<p>正文</p>", "top", 1, "status", 1,
                "publishedAt", publishedAt.format(f))), adminToken())).get("id").asLong();
    }

    @Test
    void 定时新闻到点后_列表缓存被清掉_一分钟内可见() throws Exception {
        long id = createNews("到点自动出现", LocalDateTime.now().plusHours(1));
        assertThat(ok(get("/api/portal/news")).get("list").findValuesAsText("title")).doesNotContain("到点自动出现");  // 写入缓存
        // 模拟时间走到发布时刻：直接改库，不经过后台接口（后台接口本身会清缓存）
        newsMapper.update(new LambdaUpdateWrapper<News>().eq(News::getId, id).set(News::getPublishedAt, LocalDateTime.now().minusSeconds(10)));
        assertThat(ok(get("/api/portal/news")).get("list").findValuesAsText("title")).doesNotContain("到点自动出现");  // 仍是旧缓存

        publishJob.evictIfDue(LocalDateTime.now());
        assertThat(ok(get("/api/portal/news")).get("list").findValuesAsText("title")).contains("到点自动出现");
    }

    @Test
    void 阅读数_同一IP一小时内只计一次() throws Exception {
        long id = createNews("阅读数测试", LocalDateTime.now().minusMinutes(1));
        String admin = adminToken();
        int before = ok(auth(get("/api/admin/news/" + id), admin)).get("views").asInt();
        ok(get("/api/portal/news/" + id).with(r -> { r.setRemoteAddr("10.1.0.1"); return r; }));
        ok(get("/api/portal/news/" + id).with(r -> { r.setRemoteAddr("10.1.0.1"); return r; }));
        ok(get("/api/portal/news/" + id).with(r -> { r.setRemoteAddr("10.1.0.2"); return r; }));
        assertThat(ok(auth(get("/api/admin/news/" + id), admin)).get("views").asInt()).isEqualTo(before + 2);
    }
    @Test
    void 种子新闻立即可见_时区一致() throws Exception {
        JsonNode page = ok(get("/api/portal/news"));
        assertThat(page.get("total").asInt()).isGreaterThanOrEqualTo(1);
        // 按标题找种子新闻（其他测试会新增置顶新闻，不能假定它排第一）
        long id = 0;
        for (JsonNode n : page.get("list")) if (n.get("title").asText().equals("一零科技官网全新改版上线")) id = n.get("id").asLong();
        assertThat(id).isPositive();
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
