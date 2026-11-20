package com.cn01.admin;

import com.cn01.auth.AuthContext;
import com.cn01.common.RequestUtils;
import com.cn01.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService service;

    public record LoginReq(@NotBlank(message = "请输入账号") @Size(max = 50) String username,
                           @NotBlank(message = "请输入密码") @Size(max = 64) String password) {}

    public record Me(long id, String username, String realName) {}

    public record PasswordReq(@NotBlank(message = "请输入原密码") String oldPassword,
                              @NotBlank(message = "请输入新密码")
                              @Size(min = 8, max = 32, message = "新密码长度 8-32 位")
                              @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码需同时包含字母和数字") String newPassword) {}

    @PostMapping("/login")
    public Result<AdminAuthService.LoginResult> login(@Valid @RequestBody LoginReq req, HttpServletRequest http) {
        return Result.ok(service.login(req.username().trim(), req.password(), RequestUtils.clientIp(http)));
    }

    @GetMapping("/me")
    public Result<Me> me() {
        AdminUser u = service.require(AuthContext.requireAdmin().id());
        return Result.ok(new Me(u.getId(), u.getUsername(), u.getRealName()));
    }

    @PutMapping("/password")
    public Result<AdminAuthService.LoginResult> password(@Valid @RequestBody PasswordReq req) {
        return Result.ok(service.changePassword(AuthContext.requireAdmin().id(), req.oldPassword(), req.newPassword()));
    }
}
