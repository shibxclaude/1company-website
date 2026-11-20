package com.cn01.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.notify.NotificationService;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.entity.PaymentNotify;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.order.mapper.PaymentNotifyMapper;
import com.cn01.order.service.OrderAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付成功入账（微信回调、主动查单、模拟支付共用）。
 *
 * 幂等：用"带条件的 UPDATE"把订单从待支付改为已支付——只有第一次能改成功（影响行数 = 1），
 * 重复回调影响行数为 0，判定为重复，直接返回成功、不重复入账、不重复发通知；
 * orders.transaction_id 上还有唯一索引兜底。每次回调都记一条 payment_notify 便于审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaidHandler {
    public enum Outcome { APPLIED, DUPLICATE, REJECTED, IGNORED }

    private final OrderMapper orderMapper;
    private final PaymentNotifyMapper notifyMapper;
    private final OrderAssembler assembler;
    private final NotificationService notificationService;

    @Transactional
    public Outcome onPaid(String channel, String orderNo, String transactionId, String tradeState, long amountFen, String raw) {
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (o == null) return record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.REJECTED, "订单不存在", raw);
        if (!"SUCCESS".equals(tradeState)) return record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.IGNORED, "非成功状态", raw);

        long expected = PayClient.toFen(o.getTotalAmount());
        if (expected != amountFen) {
            log.error("支付金额不符，拒绝入账 orderNo={} 应付={}分 实付={}分 tx={}", orderNo, expected, amountFen, transactionId);
            return record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.REJECTED, "金额不符，应付 " + expected + " 分", raw);
        }

        OrderStatus from = o.statusEnum();
        LocalDateTime now = LocalDateTime.now();
        // 超时关单后才到账的，同样入账（钱已经收到），并在日志中注明
        int updated = orderMapper.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, o.getId())
                .in(Order::getStatus, List.of(OrderStatus.PENDING_PAY.name(), OrderStatus.CANCELLED.name()))
                .isNull(Order::getTransactionId)
                .set(Order::getStatus, OrderStatus.PAID.name())
                .set(Order::getPaidAt, now)
                .set(Order::getTransactionId, transactionId)
                .set(Order::getPayChannel, channel));
        if (updated == 0) {
            Order latest = orderMapper.selectById(o.getId());
            if (transactionId.equals(latest.getTransactionId())) {
                return record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.DUPLICATE, "重复通知", raw);
            }
            log.error("订单已由其他交易支付，疑似重复付款，需人工退款 orderNo={} 已入账tx={} 新tx={}", orderNo, latest.getTransactionId(), transactionId);
            return record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.REJECTED, "订单已被另一笔交易支付，需人工处理退款", raw);
        }
        String note = from == OrderStatus.CANCELLED ? "支付成功（订单超时关闭后到账，已恢复为已支付）" : "支付成功";
        assembler.log(o.getId(), from, OrderStatus.PAID, note + "，交易号 " + transactionId, "SYSTEM", channel);
        record(channel, orderNo, transactionId, tradeState, amountFen, Outcome.APPLIED, null, raw);
        long orderId = o.getId();
        // 事务提交后再异步发通知，避免通知读到未提交数据或事务回滚却发了通知
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.orderPaid(orderId);
            }
        });
        log.info("订单已支付 orderNo={} tx={} channel={}", orderNo, transactionId, channel);
        return Outcome.APPLIED;
    }

    private Outcome record(String channel, String orderNo, String tx, String state, long fen, Outcome result, String note, String raw) {
        PaymentNotify n = new PaymentNotify();
        n.setChannel(channel);
        n.setOrderNo(orderNo);
        n.setTransactionId(tx == null ? "" : tx);
        n.setTradeState(state == null ? "" : state);
        n.setAmountFen(fen);
        n.setResult(result.name());
        n.setNote(note);
        n.setRaw(raw);
        notifyMapper.insert(n);
        return result;
    }
}
