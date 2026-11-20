package com.cn01.order.entity;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAY("待支付"),
    PAID("已支付"),
    IN_SERVICE("服务进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) { this.label = label; }

    public String label() { return label; }

    /** 后台可手动推进的状态（支付与关单由系统完成） */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = Map.of(
            PAID, Set.of(IN_SERVICE, COMPLETED),
            IN_SERVICE, Set.of(COMPLETED),
            PENDING_PAY, Set.of(CANCELLED));

    public boolean adminCanMoveTo(OrderStatus to) {
        return ADMIN_TRANSITIONS.getOrDefault(this, Set.of()).contains(to);
    }
}
