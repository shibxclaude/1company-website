package com.cn01.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.auth.AuthPrincipal;
import com.cn01.auth.JwtService;
import com.cn01.auth.LoginAttemptGuard;
import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import com.cn01.common.Status;
import com.cn01.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {
    public static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final AdminUserMapper mapper;
    private final JwtService jwt;
    private final LoginAttemptGuard loginGuard;
    private final AppProperties props;

    public record LoginResult(String token, String username, String realName) {}

    public LoginResult login(String username, String password, String ip) {
        loginGuard.check("admin", username, ip);
        AdminUser u = mapper.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (u == null || !ENCODER.matches(password, u.getPasswordHash())) {
            loginGuard.failed("admin", username, ip);
            log.warn("后台登录失败 username={} ip={}", username, ip);
            throw new BizException(ErrorCode.BAD_REQUEST, "账号或密码错误");
        }
        if (u.getStatus() != Status.ON) throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        loginGuard.succeeded("admin", username, ip);
        mapper.update(new LambdaUpdateWrapper<AdminUser>().eq(AdminUser::getId, u.getId()).set(AdminUser::getLastLoginAt, LocalDateTime.now()));
        String token = jwt.issue(new AuthPrincipal(u.getId(), AuthPrincipal.Type.ADMIN, u.getUsername()));
        return new LoginResult(token, u.getUsername(), u.getRealName());
    }

    public AdminUser require(long id) {
        AdminUser u = mapper.selectById(id);
        if (u == null || u.getStatus() != Status.ON) throw new BizException(ErrorCode.UNAUTHORIZED);
        return u;
    }

    public void changePassword(long id, String oldPassword, String newPassword) {
        AdminUser u = require(id);
        if (!ENCODER.matches(oldPassword, u.getPasswordHash())) throw BizException.badRequest("原密码不正确");
        mapper.update(new LambdaUpdateWrapper<AdminUser>().eq(AdminUser::getId, id).set(AdminUser::getPasswordHash, ENCODER.encode(newPassword)));
    }

    /** 首次启动、库里没有管理员时创建 admin。密码取 ADMIN_INIT_PASSWORD，未配置则随机生成并打印一次。 */
    @EventListener(ApplicationReadyEvent.class)
    public void initAdmin() {
        if (mapper.selectCount(null) > 0) return;
        String pwd = props.getAdmin().getInitPassword();
        boolean generated = !StringUtils.hasText(pwd);
        if (generated) pwd = randomPassword();
        AdminUser u = new AdminUser();
        u.setUsername("admin");
        u.setRealName("管理员");
        u.setPasswordHash(ENCODER.encode(pwd));
        u.setStatus(Status.ON);
        mapper.insert(u);
        if (generated) log.warn("已创建初始管理员 admin，随机密码：{}（只显示这一次，请登录后立即修改）", pwd);
        else log.info("已创建初始管理员 admin（密码来自 ADMIN_INIT_PASSWORD）");
    }

    private static String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
