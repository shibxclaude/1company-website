package com.cn01.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn01.common.BizException;
import com.cn01.common.PageResult;
import com.cn01.order.dto.OrderDtos.*;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderAdminService {
    private final OrderMapper orderMapper;
    private final OrderAssembler assembler;
    private final OrderService orderService;

    public PageResult<AdminListItem> page(String status, String keyword, LocalDate from, LocalDate to, long page, long size) {
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .ge(from != null, Order::getCreatedAt, from == null ? null : from.atStartOfDay())
                .lt(to != null, Order::getCreatedAt, to == null ? null : to.plusDays(1).atStartOfDay())
                .and(StringUtils.hasText(keyword), w -> w.like(Order::getOrderNo, keyword).or().like(Order::getContactPhone, keyword)
                        .or().like(Order::getContactName, keyword).or().like(Order::getCompanyName, keyword))
                .orderByDesc(Order::getId);
        Page<Order> p = orderMapper.selectPage(Page.of(page, size), q);
        return PageResult.of(p, o -> new AdminListItem(o.getOrderNo(), o.getStatus(), o.statusEnum().label(), o.getSolutionName(),
                o.getTotalAmount(), o.getContactName(), o.getContactPhone(), o.getCompanyName(), o.getUserId(), o.getPayChannel(),
                o.getCreatedAt(), o.getPaidAt()));
    }

    public AdminDetail detail(String orderNo) {
        Order o = require(orderNo);
        return toDetail(o);
    }

    /** 推进服务进度：可只写备注（toStatus 为空），也可同时改状态 */
    @Transactional
    public AdminDetail progress(String orderNo, ProgressReq req, String adminName) {
        Order o = require(orderNo);
        OrderStatus from = o.statusEnum();
        if (!StringUtils.hasText(req.toStatus())) {
            if (!StringUtils.hasText(req.note())) throw BizException.badRequest("请填写进度备注或选择新状态");
            assembler.log(o.getId(), from, from, req.note().trim(), "ADMIN", adminName);
            return toDetail(o);
        }
        OrderStatus to;
        try {
            to = OrderStatus.valueOf(req.toStatus());
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("状态不正确");
        }
        if (!from.adminCanMoveTo(to)) throw BizException.conflict("不能从「" + from.label() + "」改为「" + to.label() + "」");
        if (to == OrderStatus.CANCELLED) orderService.closeRemote(o);

        LambdaUpdateWrapper<Order> u = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, o.getId()).eq(Order::getStatus, from.name())
                .set(Order::getStatus, to.name());
        if (to == OrderStatus.COMPLETED) u.set(Order::getCompletedAt, LocalDateTime.now());
        if (to == OrderStatus.CANCELLED) u.set(Order::getCancelledAt, LocalDateTime.now()).set(Order::getCancelReason, "管理员取消");
        if (orderMapper.update(u) == 0) throw BizException.conflict(null);
        assembler.log(o.getId(), from, to, StringUtils.hasText(req.note()) ? req.note().trim() : null, "ADMIN", adminName);
        return toDetail(orderMapper.selectById(o.getId()));
    }

    private AdminDetail toDetail(Order o) {
        List<String> next = Arrays.stream(OrderStatus.values()).filter(o.statusEnum()::adminCanMoveTo).map(Enum::name).toList();
        return new AdminDetail(assembler.view(o), o.getUserId(), o.getTransactionId(), next);
    }

    private Order require(String orderNo) {
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (o == null) throw BizException.notFound("订单不存在");
        return o;
    }
}
