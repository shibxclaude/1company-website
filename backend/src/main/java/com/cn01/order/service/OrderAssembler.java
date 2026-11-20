package com.cn01.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn01.order.dto.OrderDtos.*;
import com.cn01.order.entity.*;
import com.cn01.order.mapper.OrderItemMapper;
import com.cn01.order.mapper.OrderLogMapper;
import com.cn01.solution.entity.Solution;
import com.cn01.solution.mapper.SolutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderAssembler {
    private final OrderItemMapper itemMapper;
    private final OrderLogMapper logMapper;
    private final SolutionMapper solutionMapper;

    public OrderView view(Order o) {
        List<ItemView> items = itemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()).orderByAsc(OrderItem::getId))
                .stream().map(i -> new ItemView(i.getItemType(), i.getItemName(), i.getBillingType(), i.getUnit(),
                        i.getUnitPrice(), i.getQuantity(), i.getAmount())).toList();
        List<LogView> logs = logMapper.selectList(new LambdaQueryWrapper<OrderLog>().eq(OrderLog::getOrderId, o.getId()).orderByAsc(OrderLog::getId))
                .stream().map(l -> new LogView(l.getFromStatus(), l.getToStatus(), OrderStatus.valueOf(l.getToStatus()).label(),
                        l.getNote(), l.getOperatorType(), l.getCreatedAt())).toList();
        Solution s = solutionMapper.selectById(o.getSolutionId());
        return new OrderView(o.getOrderNo(), o.getStatus(), o.statusEnum().label(), o.getSolutionName(), s == null ? null : s.getSlug(),
                o.getTotalAmount(), o.getContactName(), o.getContactPhone(), o.getContactEmail(), o.getCompanyName(), o.getRemark(),
                o.getAgreementVersion(), o.getPayChannel(), o.getPayExpireAt(), o.getPaidAt(), o.getCancelledAt(), o.getCancelReason(),
                o.getCompletedAt(), o.getCreatedAt(), LocalDateTime.now().withNano(0), items, logs);
    }

    /** 列表页的一行摘要，如 "标准版 × 2 + 2 项增值服务" */
    public Map<Long, String> itemSummaries(List<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        return itemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    var version = e.getValue().stream().filter(i -> OrderItem.VERSION.equals(i.getItemType())).findFirst();
                    long services = e.getValue().stream().filter(i -> OrderItem.SERVICE.equals(i.getItemType())).count();
                    String s = version.map(v -> v.getItemName() + " × " + v.getQuantity() + v.getUnit()).orElse("");
                    return services > 0 ? s + " + " + services + " 项增值服务" : s;
                }));
    }

    public void log(long orderId, OrderStatus from, OrderStatus to, String note, String operatorType, String operatorName) {
        OrderLog l = new OrderLog();
        l.setOrderId(orderId);
        l.setFromStatus(from == null ? null : from.name());
        l.setToStatus(to.name());
        l.setNote(note);
        l.setOperatorType(operatorType);
        l.setOperatorName(operatorName);
        logMapper.insert(l);
    }
}
