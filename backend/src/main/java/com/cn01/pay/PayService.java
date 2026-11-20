package com.cn01.pay;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.common.BizException;
import com.cn01.order.dto.OrderDtos.PayInfo;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PayService {
    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final PayClient payClient;

    /** 获取支付二维码：有效期内重复进入支付页复用同一个 code_url */
    public PayInfo prepare(String orderNo, String token) {
        Order o = orderService.requireAccessible(orderNo, token);
        if (o.statusEnum() != OrderStatus.PENDING_PAY) throw BizException.conflict("订单当前状态为「" + o.statusEnum().label() + "」，无需支付");
        LocalDateTime now = LocalDateTime.now();
        if (!o.getPayExpireAt().isAfter(now.plusSeconds(10))) throw BizException.conflict("订单已超时，请重新下单");

        String codeUrl = o.getCodeUrl();
        if (codeUrl == null || !payClient.channel().equals(o.getPayChannel())) {
            codeUrl = payClient.createNative(o.getOrderNo(), "一零科技-" + o.getSolutionName(), o.getTotalAmount(), o.getPayExpireAt());
            orderMapper.update(new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, o.getId()).eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                    .set(Order::getCodeUrl, codeUrl).set(Order::getPayChannel, payClient.channel()));
        }
        return new PayInfo(codeUrl, payClient.channel(), payClient.isMock(), o.getTotalAmount(), o.getPayExpireAt(), now.withNano(0));
    }
}
