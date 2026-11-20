package com.cn01.auth;

import com.cn01.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {
    private final SecretKey key;
    private final AppProperties.Jwt cfg;

    public JwtService(AppProperties props) {
        this.cfg = props.getJwt();
        String secret = cfg.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("未配置 JWT_SECRET（或不足 32 字节），已使用随机密钥：重启后所有登录失效。生产环境务必配置。");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String issue(AuthPrincipal p) {
        int hours = p.type() == AuthPrincipal.Type.ADMIN ? cfg.getAdminTtlHours() : cfg.getUserTtlHours();
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(p.id()))
                .claim("typ", p.type().name())
                .claim("name", p.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(hours))))
                .signWith(key)
                .compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthPrincipal(Long.parseLong(c.getSubject()),
                    AuthPrincipal.Type.valueOf(c.get("typ", String.class)), c.get("name", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
