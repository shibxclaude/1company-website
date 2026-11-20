package com.cn01.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.admin.AdminAuthService;
import com.cn01.auth.AuthPrincipal;
import com.cn01.auth.JwtService;
import com.cn01.auth.LoginAttemptGuard;
import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import com.cn01.common.Status;
import com.cn01.sms.SmsCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuthService {
    private final AppUserMapper mapper;
    private final SmsCodeService sms;
    private final JwtService jwt;
    private final LoginAttemptGuard loginGuard;

    public record LoginResult(String token, UserView user) {}

    public record UserView(long id, String phone, String username, String nickname, boolean hasPassword) {
        static UserView of(AppUser u) {
            return new UserView(u.getId(), u.getPhone(), u.getUsername(), u.getNickname(), u.getPasswordHash() != null);
        }
    }

    public LoginResult register(String phone, String code, String password, String username) {
        sms.verify(phone, SmsCodeService.Scene.REGISTER, code);
        if (exists(AppUser::getPhone, phone)) throw BizException.conflict("该手机号已注册，请直接登录");
        if (StringUtils.hasText(username) && exists(AppUser::getUsername, username)) throw BizException.conflict("账号已被占用");
        AppUser u = new AppUser();
        u.setPhone(phone);
        u.setUsername(StringUtils.hasText(username) ? username : null);
        u.setPasswordHash(AdminAuthService.ENCODER.encode(password));
        u.setNickname(maskPhone(phone));
        u.setStatus(Status.ON);
        u.setLastLoginAt(LocalDateTime.now());
        try {
            mapper.insert(u);
        } catch (DuplicateKeyException e) {
            throw BizException.conflict("该手机号或账号已注册");
        }
        return issue(u);
    }

    /** 验证码登录；手机号未注册时自动注册（无密码，可稍后在用户中心设置） */
    public LoginResult loginBySms(String phone, String code) {
        sms.verify(phone, SmsCodeService.Scene.LOGIN, code);
        AppUser u = mapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (u == null) {
            u = new AppUser();
            u.setPhone(phone);
            u.setNickname(maskPhone(phone));
            u.setStatus(Status.ON);
            try {
                mapper.insert(u);
            } catch (DuplicateKeyException e) {
                u = mapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
            }
        }
        return afterLogin(u);
    }

    /** 账号密码登录：account 可以是手机号或账号 */
    public LoginResult loginByPassword(String account, String password, String ip) {
        loginGuard.check("user", account, ip);
        AppUser u = mapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, account).or().eq(AppUser::getUsername, account).last("LIMIT 1"));
        if (u == null || u.getPasswordHash() == null || !AdminAuthService.ENCODER.matches(password, u.getPasswordHash())) {
            loginGuard.failed("user", account, ip);
            throw BizException.badRequest("账号或密码错误");
        }
        loginGuard.succeeded("user", account, ip);
        return afterLogin(u);
    }

    public UserView me(long id) {
        return UserView.of(requireActive(id));
    }

    /** 设置/修改密码；之前签发的所有 token 失效，返回给当前会话一个新 token */
    public LoginResult setPassword(long id, String oldPassword, String newPassword) {
        AppUser u = requireActive(id);
        if (u.getPasswordHash() != null && (oldPassword == null || !AdminAuthService.ENCODER.matches(oldPassword, u.getPasswordHash())))
            throw BizException.badRequest("原密码不正确");
        mapper.update(new LambdaUpdateWrapper<AppUser>().eq(AppUser::getId, id)
                .set(AppUser::getPasswordHash, AdminAuthService.ENCODER.encode(newPassword))
                .setSql("token_version = token_version + 1"));
        return issue(mapper.selectById(id));
    }

    public AppUser requireActive(long id) {
        AppUser u = mapper.selectById(id);
        if (u == null || u.getStatus() != Status.ON) throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        return u;
    }

    private LoginResult afterLogin(AppUser u) {
        if (u.getStatus() != Status.ON) throw new BizException(ErrorCode.FORBIDDEN, "账号已被停用，请联系客服");
        mapper.update(new LambdaUpdateWrapper<AppUser>().eq(AppUser::getId, u.getId()).set(AppUser::getLastLoginAt, LocalDateTime.now()));
        return issue(u);
    }

    private LoginResult issue(AppUser u) {
        return new LoginResult(jwt.issue(new AuthPrincipal(u.getId(), AuthPrincipal.Type.USER, u.getPhone(), AuthPrincipal.ver(u.getTokenVersion()))), UserView.of(u));
    }

    private boolean exists(com.baomidou.mybatisplus.core.toolkit.support.SFunction<AppUser, ?> col, String v) {
        return mapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(col, v)) > 0;
    }

    static String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
