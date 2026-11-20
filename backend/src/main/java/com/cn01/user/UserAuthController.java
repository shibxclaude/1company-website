package com.cn01.user;

import com.cn01.auth.AuthContext;
import com.cn01.common.RequestUtils;
import com.cn01.common.Result;
import com.cn01.sms.SmsCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserAuthController {
    static final String PHONE = "^1[3-9]\\d{9}$";
    static final String PWD = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$";

    private final UserAuthService service;
    private final SmsCodeService sms;

    public record SmsReq(@NotBlank(message = "请输入手机号") @Pattern(regexp = PHONE, message = "手机号格式不正确") String phone,
                         @NotNull(message = "缺少场景") SmsCodeService.Scene scene) {}

    public record SmsResp(int interval, String mockCode) {}

    public record RegisterReq(@NotBlank(message = "请输入手机号") @Pattern(regexp = PHONE, message = "手机号格式不正确") String phone,
                              @NotBlank(message = "请输入验证码") @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字") String code,
                              @NotBlank(message = "请设置密码") @Pattern(regexp = PWD, message = "密码 8-32 位，需同时包含字母和数字") String password,
                              @Pattern(regexp = "^$|^[A-Za-z][A-Za-z0-9_]{3,19}$", message = "账号以字母开头，4-20 位字母、数字或下划线") String username) {}

    public record SmsLoginReq(@NotBlank(message = "请输入手机号") @Pattern(regexp = PHONE, message = "手机号格式不正确") String phone,
                              @NotBlank(message = "请输入验证码") @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字") String code) {}

    public record PasswordLoginReq(@NotBlank(message = "请输入手机号或账号") @Size(max = 50) String account,
                                   @NotBlank(message = "请输入密码") @Size(max = 64) String password) {}

    public record SetPasswordReq(String oldPassword,
                                 @NotBlank(message = "请输入新密码") @Pattern(regexp = PWD, message = "密码 8-32 位，需同时包含字母和数字") String newPassword) {}

    @PostMapping("/api/portal/sms/code")
    public Result<SmsResp> sendCode(@Valid @RequestBody SmsReq req, HttpServletRequest http) {
        return Result.ok(new SmsResp(60, sms.send(req.phone(), req.scene(), RequestUtils.clientIp(http))));
    }

    @PostMapping("/api/portal/auth/register")
    public Result<UserAuthService.LoginResult> register(@Valid @RequestBody RegisterReq req) {
        return Result.ok(service.register(req.phone(), req.code(), req.password(), req.username()));
    }

    @PostMapping("/api/portal/auth/login/sms")
    public Result<UserAuthService.LoginResult> loginSms(@Valid @RequestBody SmsLoginReq req) {
        return Result.ok(service.loginBySms(req.phone(), req.code()));
    }

    @PostMapping("/api/portal/auth/login/password")
    public Result<UserAuthService.LoginResult> loginPassword(@Valid @RequestBody PasswordLoginReq req, HttpServletRequest http) {
        return Result.ok(service.loginByPassword(req.account().trim(), req.password(), RequestUtils.clientIp(http)));
    }

    @GetMapping("/api/user/me")
    public Result<UserAuthService.UserView> me() {
        return Result.ok(service.me(AuthContext.requireUser().id()));
    }

    @PutMapping("/api/user/password")
    public Result<Void> setPassword(@Valid @RequestBody SetPasswordReq req) {
        service.setPassword(AuthContext.requireUser().id(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }
}
