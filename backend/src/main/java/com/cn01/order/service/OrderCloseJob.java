package com.cn01.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.pay.PaidHandler;
import com.cn01.pay.PayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 30 分钟未支付自动关单。每分钟扫描一次；多实例部署时用 Redis 锁保证同一时刻只有一个实例在跑。
 * 关单前先向支付平台查单：如果其实已经付了（回调丢失），按已支付入账而不是关闭；
 * 查单失败或用户正在支付（结果不确定）时本轮跳过，下一轮再查，绝不在不确定时关单。
 *
 * 对账：每小时把最近 48 小时内关闭、生成过二维码却没有交易号的订单再查一遍，
 * 找回"关单时查不到、回调又丢了"的已付款订单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseJob {
    private final OrderMapper orderMapper;
    private final OrderAssembler assembler;
    private final PayClient payClient;
    private final PaidHandler paidHandler;
    private final StringRedisTemplate redis;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        Boolean locked = redis.opsForValue().setIfAbsent("cn01:lock:order-close", "1", Duration.ofSeconds(55));
        if (!Boolean.TRUE.equals(locked)) return;
        closeExpired(LocalDateTime.now());
    }

    /** @return 本次关闭的订单数 */
    public int closeExpired(LocalDateTime now) {
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                .le(Order::getPayExpireAt, now)
                .last("LIMIT 200"));
        int closed = 0;
        for (Order o : expired) {
            try {
                if (o.getCodeUrl() != null) {
                    var q = payClient.query(o.getOrderNo());
                    if (q.state() == PayClient.QueryResult.State.PAID) {
                        var r = q.paid();
                        paidHandler.onPaid(payClient.channel(), o.getOrderNo(), r.transactionId(), "SUCCESS", r.amountFen(), r.raw());
                        continue;
                    }
                    if (q.state() == PayClient.QueryResult.State.UNKNOWN) {
                        log.warn("查单结果不确定，本轮不关单 orderNo={}", o.getOrderNo());
                        continue;
                    }
                    payClient.close(o.getOrderNo());
                }
                int n = orderMapper.update(new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, o.getId()).eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                        .set(Order::getStatus, OrderStatus.CANCELLED.name())
                        .set(Order::getCancelledAt, now)
                        .set(Order::getCancelReason, "超时未支付"));
                if (n == 1) {
                    assembler.log(o.getId(), OrderStatus.PENDING_PAY, OrderStatus.CANCELLED, "超时未支付，系统自动关闭", "SYSTEM", null);
                    closed++;
                }
            } catch (Exception e) {
                log.error("自动关单失败 orderNo={}", o.getOrderNo(), e);
            }
        }
        if (closed > 0) log.info("自动关闭超时订单 {} 笔", closed);
        return closed;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void runReconcile() {
        Boolean locked = redis.opsForValue().setIfAbsent("cn01:lock:order-reconcile", "1", Duration.ofMinutes(50));
        if (!Boolean.TRUE.equals(locked)) return;
        reconcile(LocalDateTime.now());
    }

    /** @return 本次找回（恢复为已支付）的订单数 */
    public int reconcile(LocalDateTime now) {
        List<Order> candidates = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.CANCELLED.name())
                .isNotNull(Order::getCodeUrl)
                .isNull(Order::getTransactionId)
                .ge(Order::getCancelledAt, now.minusHours(48))
                .orderByDesc(Order::getId)
                .last("LIMIT 500"));
        int recovered = 0;
        for (Order o : candidates) {
            try {
                var q = payClient.query(o.getOrderNo());
                if (q.state() != PayClient.QueryResult.State.PAID) continue;
                var r = q.paid();
                if (paidHandler.onPaid(payClient.channel(), o.getOrderNo(), r.transactionId(), "SUCCESS", r.amountFen(), r.raw())
                        == PaidHandler.Outcome.APPLIED) {
                    log.warn("对账找回已付款订单 orderNo={} tx={}", o.getOrderNo(), r.transactionId());
                    recovered++;
                }
            } catch (Exception e) {
                log.error("对账失败 orderNo={}", o.getOrderNo(), e);
            }
        }
        return recovered;
    }
}
