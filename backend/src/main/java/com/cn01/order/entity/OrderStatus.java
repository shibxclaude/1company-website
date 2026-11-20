package com.cn01.order.entity;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAY("待支付"),
    PAID("已支付"),
    IN_SERVICE("服务进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    /** 全额退款成功（由退款流程置位，不能在后台手动改） */
    REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    public String label() { return label; }

    /** 可以发起订单退款的状态（已完成的订单不退） */
    public boolean refundable() {
        return this == PAID || this == IN_SERVICE;
    }

    /** 后台可手动推进的状态（支付、关单、退款由系统完成） */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = Map.of(
            PAID, Set.of(IN_SERVICE, COMPLETED),
            IN_SERVICE, Set.of(COMPLETED),
            PENDING_PAY, Set.of(CANCELLED));

    public boolean adminCanMoveTo(OrderStatus to) {
        return ADMIN_TRANSITIONS.getOrDefault(this, Set.of()).contains(to);
    }
}
