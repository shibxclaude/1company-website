package com.cn01.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn01.common.BizException;
import com.cn01.common.PageResult;
import com.cn01.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/** 后台用户管理：列表、启用/停用 */
@Validated
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminAppUserController {
    private final AppUserMapper mapper;

    public record Row(long id, String phone, String username, String nickname, boolean hasPassword, int status,
                      LocalDateTime lastLoginAt, LocalDateTime createdAt) {}

    @GetMapping
    public Result<PageResult<Row>> page(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") @Min(1) long page,
                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        Page<AppUser> p = mapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<AppUser>()
                .and(StringUtils.hasText(keyword), w -> w.like(AppUser::getPhone, keyword).or().like(AppUser::getUsername, keyword))
                .eq(status != null, AppUser::getStatus, status)
                .orderByDesc(AppUser::getId));
        return Result.ok(PageResult.of(p, u -> new Row(u.getId(), u.getPhone(), u.getUsername(), u.getNickname(),
                u.getPasswordHash() != null, u.getStatus(), u.getLastLoginAt(), u.getCreatedAt())));
    }

    public record StatusReq(@Min(0) @Max(1) int status) {}

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable long id, @Valid @RequestBody StatusReq req) {
        if (mapper.selectById(id) == null) throw BizException.notFound("用户不存在");
        mapper.update(new LambdaUpdateWrapper<AppUser>().eq(AppUser::getId, id).set(AppUser::getStatus, req.status()));
        return Result.ok();
    }
}
