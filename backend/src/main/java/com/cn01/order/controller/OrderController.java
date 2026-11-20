package com.cn01.order.controller;

import com.cn01.auth.AuthContext;
import com.cn01.common.PageResult;
import com.cn01.common.RequestUtils;
import com.cn01.common.Result;
import com.cn01.order.dto.OrderDtos.*;
import com.cn01.order.service.OrderAdminService;
import com.cn01.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderAdminService adminService;

    // ---------- 前台：游客可下单，凭 token 查看/支付（token 优先放请求头 X-Order-Token；?token= 仅为兼容旧前端保留，会进访问日志） ----------

    @PostMapping("/api/portal/orders")
    public Result<Created> create(@Valid @RequestBody CreateReq req, HttpServletRequest http) {
        return Result.ok(orderService.create(req, RequestUtils.clientIp(http)));
    }

    @GetMapping("/api/portal/orders/{orderNo}")
    public Result<OrderView> view(@PathVariable String orderNo, @RequestHeader(value = "X-Order-Token", required = false) String headerToken, @RequestParam(required = false) String token) {
        return Result.ok(orderService.view(orderNo, OrderService.pickToken(headerToken, token)));
    }

    @GetMapping("/api/portal/orders/{orderNo}/status")
    public Result<StatusView> status(@PathVariable String orderNo, @RequestHeader(value = "X-Order-Token", required = false) String headerToken, @RequestParam(required = false) String token) {
        return Result.ok(orderService.status(orderNo, OrderService.pickToken(headerToken, token)));
    }

    @PostMapping("/api/portal/orders/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo, @RequestHeader(value = "X-Order-Token", required = false) String headerToken, @RequestParam(required = false) String token) {
        orderService.cancelByUser(orderNo, OrderService.pickToken(headerToken, token));
        return Result.ok();
    }

    // ---------- 用户中心 ----------

    @GetMapping("/api/user/orders")
    public Result<PageResult<ListItem>> myOrders(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") @Min(1) long page,
                                                 @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size) {
        return Result.ok(orderService.myOrders(AuthContext.requireUser().id(), status, page, size));
    }

    // ---------- 后台 ----------

    @GetMapping("/api/admin/orders")
    public Result<PageResult<AdminListItem>> adminPage(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                       @RequestParam(defaultValue = "1") @Min(1) long page,
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.ok(adminService.page(status, keyword, from, to, page, size));
    }

    @GetMapping("/api/admin/orders/{orderNo}")
    public Result<AdminDetail> adminDetail(@PathVariable String orderNo) {
        return Result.ok(adminService.detail(orderNo));
    }

    @PostMapping("/api/admin/orders/{orderNo}/progress")
    public Result<AdminDetail> progress(@PathVariable String orderNo, @Valid @RequestBody ProgressReq req) {
        return Result.ok(adminService.progress(orderNo, req, AuthContext.requireAdmin().name()));
    }
}
