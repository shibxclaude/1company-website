package com.cn01;

import com.cn01.admin.AdminUser;
import com.cn01.admin.AdminUserMapper;
import com.cn01.auth.AuthInterceptor;
import com.cn01.auth.AuthPrincipal;
import com.cn01.auth.JwtService;
import com.cn01.auth.RoleGuard;
import com.cn01.common.Status;
import com.cn01.config.AppProperties;
import com.cn01.user.AppUser;
import com.cn01.user.AppUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 鉴权不能被路径变形绕过：在真实的内嵌 Tomcat 上，用与 WebMvcConfig 相同的 RoleGuard.register 注册拦截器，
 * 验证 ;参数、百分号编码等能路由到后台/用户中心接口的写法同样要求登录。不依赖数据库（账号表用 mock）。
 */
@SpringBootTest(classes = AuthPathTests.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthPathTests {

    @SpringBootConfiguration
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class App implements WebMvcConfigurer {
        static final AdminUserMapper ADMINS = mock(AdminUserMapper.class);
        static final AppUserMapper USERS = mock(AppUserMapper.class);

        @RestController
        static class Endpoints {
            @GetMapping("/api/admin/orders") String adminOrders() { return "ADMIN-DATA"; }
            @PostMapping("/api/admin/auth/login") String adminLogin() { return "LOGIN-PAGE"; }
            @GetMapping("/api/user/me") String userMe() { return "USER-DATA"; }
            @GetMapping("/api/portal/solutions") String portal() { return "PUBLIC"; }
        }

        @Bean JwtService jwtService() {
            AppProperties p = new AppProperties();
            p.getJwt().setSecret("0123456789abcdef0123456789abcdef-test");
            return new JwtService(p);
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            RoleGuard.register(registry, new AuthInterceptor(jwtService(), ADMINS, USERS), new ObjectMapper());
        }
    }

    @LocalServerPort int port;
    @Autowired JwtService jwt;
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> call(String method, String path, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/orders", "/api/admin;x=1/orders", "/api/admin/orders;x=1", "/api/%61dmin/orders", "/api/a%64min/orders"})
    void 后台接口_各种路径写法未登录都是401(String path) throws Exception {
        var r = call("GET", path, null);
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.body()).doesNotContain("ADMIN-DATA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/user/me", "/api/user;x=1/me", "/api/%75ser/me"})
    void 用户中心_各种路径写法未登录都是401(String path) throws Exception {
        var r = call("GET", path, null);
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.body()).doesNotContain("USER-DATA");
    }

    @Test
    void 前台用户token不能访问后台() throws Exception {
        AppUser u = new AppUser();
        u.setId(7L);
        u.setStatus(Status.ON);
        when(App.USERS.selectById(any())).thenReturn(u);
        String token = jwt.issue(new AuthPrincipal(7, AuthPrincipal.Type.USER, "13800000000", 0));
        assertThat(call("GET", "/api/admin;x/orders", token).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/api/user/me", token).body()).isEqualTo("USER-DATA");
    }

    @Test
    void 管理员token可以访问后台_登录接口与前台接口无需token() throws Exception {
        AdminUser a = new AdminUser();
        a.setId(1L);
        a.setStatus(Status.ON);
        when(App.ADMINS.selectById(any())).thenReturn(a);
        String token = jwt.issue(new AuthPrincipal(1, AuthPrincipal.Type.ADMIN, "admin", 0));
        assertThat(call("GET", "/api/admin/orders", token).body()).isEqualTo("ADMIN-DATA");
        assertThat(call("POST", "/api/admin/auth/login", null).body()).isEqualTo("LOGIN-PAGE");
        assertThat(call("GET", "/api/portal/solutions", null).body()).isEqualTo("PUBLIC");
    }
}
