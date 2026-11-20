package com.cn01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 集成测试基类：真实 MySQL 8 + Redis 容器（所有测试类共用一组，单例启动）。
 * Flyway 建表并写入种子数据；初始管理员密码固定为 Admin12345。
 */
@SpringBootTest(properties = {
        "app.admin.init-password=Admin12345",
        "app.jwt.secret=test-secret-test-secret-test-secret-0123456789",
        "app.order.create-limit-per-hour=10000",
        "app.upload.dir=target/test-uploads",
        "app.pay.mode=mock",
        "app.sms.mock=true"
})
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("cn01").withUsername("cn01").withPassword("cn01")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(Cn01Application.ZONE));
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true");
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;
    @Autowired protected StringRedisTemplate redis;

    @BeforeEach
    void clearRateLimits() {
        var keys = redis.keys("cn01:rl:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    protected JsonNode call(MockHttpServletRequestBuilder req, int expectHttp) throws Exception {
        MvcResult r = mvc.perform(req).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (r.getResponse().getStatus() != expectHttp) {
            throw new AssertionError("期望 HTTP " + expectHttp + "，实际 " + r.getResponse().getStatus() + "：" + body);
        }
        return body.isEmpty() ? null : om.readTree(body);
    }

    protected JsonNode ok(MockHttpServletRequestBuilder req) throws Exception {
        JsonNode n = call(req, 200);
        if (n.get("code").asInt() != 0) throw new AssertionError("业务失败：" + n);
        return n.get("data");
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, Object body) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
    }

    protected String adminToken() throws Exception {
        return ok(json(post("/api/admin/auth/login"), java.util.Map.of("username", "admin", "password", "Admin12345"))).get("token").asText();
    }

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }
}
