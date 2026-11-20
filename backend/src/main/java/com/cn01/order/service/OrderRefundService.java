package com.cn01.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.common.BizException;
import com.cn01.notify.NotificationService;
import com.cn01.order.dto.OrderDtos.AbnormalPayment;
import com.cn01.order.dto.OrderDtos.RefundView;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderRefund;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.entity.PaymentNotify;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.order.mapper.OrderRefundMapper;
import com.cn01.order.mapper.PaymentNotifyMapper;
import com.cn01.pay.PaidHandler;
import com.cn01.pay.PayClient;
import com.cn01.pay.PayClient.RefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 退款（后台发起，原路退回）。
 *
 * 流程：① 事务内锁订单行、校验可退额度、写一条"退款中"的退款单（额度即被占用）；
 *       ② 事务外调用支付平台退款（不在数据库事务里等外部接口）；
 *       ③ 按结果标记成功/失败；结果不确定（处理中、网络超时）的，由支付平台退款回调或每 5 分钟的同步任务确认。
 * 成功入账用"带条件的 UPDATE"（PROCESSING → SUCCESS），回调和查单重复确认也只累加一次。
 *
 * 两类退款：ORDER 退订单货款（可部分退，累计不超过实付，全额退完订单变为"已退款"）；
 *          STRAY 退回异常收款（重复付款、金额不符等没入账到订单的交易），整笔退回，不影响订单状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> ACTIVE = List.of(OrderRefund.Status.PROCESSING.name(), OrderRefund.Status.SUCCESS.name());

    private final OrderMapper orderMapper;
    private final OrderRefundMapper refundMapper;
    private final PaymentNotifyMapper notifyMapper;
    private final OrderAssembler assembler;
    private final PayClient payClient;
    private final NotificationService notificationService;
    private final TransactionTemplate tx;
    private final StringRedisTemplate redis;

    // ---------- 发起 ----------

    /** 退订单货款（可部分退款） */
    public void refundOrder(String orderNo, BigDecimal requested, String reason, String operator) {
        // 统一为两位小数（接口已限制最多两位小数），进度记录里显示为 ¥100.00 而不是 ¥100
        BigDecimal amount = requested.setScale(2, RoundingMode.UNNECESSARY);
        OrderRefund r = tx.execute(s -> {
            Order o = lockOrder(orderNo);
            if (!o.statusEnum().refundable())
                throw BizException.conflict("只有「已支付」或「服务中」的订单可以退款，当前为「" + o.statusEnum().label() + "」");
            if (o.getTransactionId() == null) throw BizException.conflict("订单没有支付交易号，无法原路退款");
            requireSameChannel(o.getPayChannel());
            BigDecimal refundable = refundable(o);
            if (amount.compareTo(refundable) > 0) throw BizException.badRequest("退款金额超过可退金额 ¥" + refundable.toPlainString());
            OrderRefund created = insert(o, OrderRefund.KIND_ORDER, o.getPayChannel(), o.getTransactionId(),
                    PayClient.toFen(o.getTotalAmount()), amount, reason, operator);
            assembler.log(o.getId(), o.statusEnum(), o.statusEnum(), "发起退款 ¥" + amount.toPlainString() + "，原因：" + reason, "ADMIN", operator);
            return created;
        });
        execute(r);
    }

    /** 退回一笔异常收款（整笔） */
    public void refundStray(String orderNo, String transactionId, String reason, String operator) {
        OrderRefund r = tx.execute(s -> {
            Order o = lockOrder(orderNo);
            PaymentNotify n = abnormalNotifies(o).get(transactionId);
            if (n == null) throw BizException.notFound("该订单没有这笔异常收款");
            if (isStrayRefunded(o.getId(), transactionId)) throw BizException.conflict("这笔异常收款已经在退或已退回");
            requireSameChannel(n.getChannel());
            BigDecimal amount = BigDecimal.valueOf(n.getAmountFen()).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
            OrderRefund created = insert(o, OrderRefund.KIND_STRAY, n.getChannel(), transactionId, n.getAmountFen(), amount, reason, operator);
            assembler.log(o.getId(), o.statusEnum(), o.statusEnum(),
                    "发起退回异常收款 ¥" + amount.toPlainString() + "（交易号 " + transactionId + "），原因：" + reason, "ADMIN", operator);
            return created;
        });
        execute(r);
    }

    private void execute(OrderRefund r) {
        RefundResult result;
        try {
            result = payClient.refund(r.getRefundNo(), r.getTransactionId(), PayClient.toFen(r.getAmount()), r.getTotalFen(), r.getReason());
        } catch (RuntimeException e) {
            // 结果未知：保持"退款中"，由回调或同步任务确认；绝不能当作失败而让管理员重复发起
            log.error("调用退款接口异常，等待同步确认 refundNo={}", r.getRefundNo(), e);
            result = RefundResult.processing(null);
        }
        apply(r.getRefundNo(), result);
    }

    // ---------- 结果确认（发起后、回调、同步任务共用） ----------

    public void apply(String refundNo, RefundResult result) {
        switch (result.state()) {
            case SUCCESS -> applySuccess(refundNo, result.refundId());
            case FAILED -> applyFailed(refundNo, result.message());
            case PROCESSING -> {
                if (result.refundId() != null) refundMapper.update(new LambdaUpdateWrapper<OrderRefund>()
                        .eq(OrderRefund::getRefundNo, refundNo).isNull(OrderRefund::getRefundId).set(OrderRefund::getRefundId, result.refundId()));
            }
        }
    }

    public void applySuccess(String refundNo, String refundId) {
        tx.executeWithoutResult(s -> {
            int n = refundMapper.update(new LambdaUpdateWrapper<OrderRefund>()
                    .eq(OrderRefund::getRefundNo, refundNo).eq(OrderRefund::getStatus, OrderRefund.Status.PROCESSING.name())
                    .set(OrderRefund::getStatus, OrderRefund.Status.SUCCESS.name())
                    .set(refundId != null, OrderRefund::getRefundId, refundId)
                    .set(OrderRefund::getFinishedAt, LocalDateTime.now()));
            OrderRefund r = requireRefund(refundNo);
            if (n == 0) {
                if (OrderRefund.Status.FAILED.name().equals(r.getStatus()))
                    log.error("退款单已标记失败，支付平台却报告成功，需人工核对 refundNo={} refundId={}", refundNo, refundId);
                return;   // 已确认过，重复通知
            }
            Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getId, r.getOrderId()).last("FOR UPDATE"));
            OrderStatus from = o.statusEnum();
            if (OrderRefund.KIND_STRAY.equals(r.getKind())) {
                assembler.log(o.getId(), from, from, "异常收款已原路退回 ¥" + r.getAmount().toPlainString() + "（交易号 " + r.getTransactionId() + "）", "SYSTEM", null);
                return;
            }
            orderMapper.update(new LambdaUpdateWrapper<Order>().eq(Order::getId, o.getId())
                    .setSql("refunded_amount = refunded_amount + " + r.getAmount().toPlainString()));
            BigDecimal refunded = orderMapper.selectById(o.getId()).getRefundedAmount();
            if (refunded.compareTo(o.getTotalAmount()) >= 0 && from.refundable()) {
                orderMapper.update(new LambdaUpdateWrapper<Order>().eq(Order::getId, o.getId())
                        .set(Order::getStatus, OrderStatus.REFUNDED.name()));
                assembler.log(o.getId(), from, OrderStatus.REFUNDED, "退款成功 ¥" + r.getAmount().toPlainString() + "，已全额退款", "SYSTEM", null);
            } else {
                assembler.log(o.getId(), from, from, "退款成功 ¥" + r.getAmount().toPlainString() + "（累计已退 ¥" + refunded.toPlainString() + "）", "SYSTEM", null);
            }
            long refundRowId = r.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.orderRefunded(refundRowId);
                }
            });
        });
    }

    public void applyFailed(String refundNo, String reason) {
        tx.executeWithoutResult(s -> {
            String msg = reason == null || reason.isBlank() ? "支付平台拒绝退款" : (reason.length() > 250 ? reason.substring(0, 250) : reason);
            int n = refundMapper.update(new LambdaUpdateWrapper<OrderRefund>()
                    .eq(OrderRefund::getRefundNo, refundNo).eq(OrderRefund::getStatus, OrderRefund.Status.PROCESSING.name())
                    .set(OrderRefund::getStatus, OrderRefund.Status.FAILED.name())
                    .set(OrderRefund::getFailReason, msg)
                    .set(OrderRefund::getFinishedAt, LocalDateTime.now()));
            if (n == 0) return;
            OrderRefund r = requireRefund(refundNo);
            Order o = orderMapper.selectById(r.getOrderId());
            assembler.log(o.getId(), o.statusEnum(), o.statusEnum(), "退款失败 ¥" + r.getAmount().toPlainString() + "：" + msg, "SYSTEM", null);
            log.warn("退款失败 refundNo={} orderNo={}：{}", refundNo, r.getOrderNo(), msg);
        });
    }

    // ---------- 同步任务 ----------

    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void runSync() {
        Boolean locked = redis.opsForValue().setIfAbsent("cn01:lock:refund-sync", "1", Duration.ofMinutes(4));
        if (!Boolean.TRUE.equals(locked)) return;
        sync(LocalDateTime.now());
    }

    /** 查询发起超过 1 分钟仍在处理中的退款。@return 本次确认（成功或失败）的退款数 */
    public int sync(LocalDateTime now) {
        List<OrderRefund> processing = refundMapper.selectList(new LambdaQueryWrapper<OrderRefund>()
                .eq(OrderRefund::getStatus, OrderRefund.Status.PROCESSING.name())
                .le(OrderRefund::getCreatedAt, now.minusMinutes(1))
                .orderByAsc(OrderRefund::getId).last("LIMIT 100"));
        int resolved = 0;
        for (OrderRefund r : processing) {
            try {
                RefundResult result = payClient.queryRefund(r.getRefundNo());
                apply(r.getRefundNo(), result);
                if (result.state() != RefundResult.State.PROCESSING) resolved++;
            } catch (Exception e) {
                log.error("同步退款结果失败 refundNo={}", r.getRefundNo(), e);
            }
        }
        return resolved;
    }

    // ---------- 后台详情用 ----------

    public BigDecimal refundable(Order o) {
        if (!o.statusEnum().refundable()) return BigDecimal.ZERO.setScale(2);
        BigDecimal used = refundMapper.selectList(new LambdaQueryWrapper<OrderRefund>()
                        .eq(OrderRefund::getOrderId, o.getId()).eq(OrderRefund::getKind, OrderRefund.KIND_ORDER).in(OrderRefund::getStatus, ACTIVE))
                .stream().map(OrderRefund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return o.getTotalAmount().subtract(used).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public List<RefundView> refunds(long orderId) {
        return refundMapper.selectList(new LambdaQueryWrapper<OrderRefund>().eq(OrderRefund::getOrderId, orderId).orderByDesc(OrderRefund::getId))
                .stream().map(r -> new RefundView(r.getRefundNo(), r.getKind(), r.getTransactionId(), r.getAmount(), r.getStatus(),
                        OrderRefund.Status.valueOf(r.getStatus()).label(), r.getReason(), r.getFailReason(), r.getOperatorName(),
                        r.getCreatedAt(), r.getFinishedAt()))
                .toList();
    }

    public List<AbnormalPayment> abnormalPayments(Order o) {
        return abnormalNotifies(o).values().stream()
                .map(n -> new AbnormalPayment(n.getTransactionId(), BigDecimal.valueOf(n.getAmountFen()).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY),
                        n.getNote(), n.getCreatedAt(), isStrayRefunded(o.getId(), n.getTransactionId())))
                .toList();
    }

    /** 被拒绝入账、但支付平台确实收了钱的交易（按交易号去重，同一笔会有多次回调） */
    private Map<String, PaymentNotify> abnormalNotifies(Order o) {
        Map<String, PaymentNotify> byTx = new LinkedHashMap<>();
        notifyMapper.selectList(new LambdaQueryWrapper<PaymentNotify>()
                        .eq(PaymentNotify::getOrderNo, o.getOrderNo())
                        .eq(PaymentNotify::getResult, PaidHandler.Outcome.REJECTED.name())
                        .eq(PaymentNotify::getTradeState, "SUCCESS")
                        .gt(PaymentNotify::getAmountFen, 0)
                        .ne(PaymentNotify::getTransactionId, "")
                        .orderByAsc(PaymentNotify::getId))
                .stream().filter(n -> !n.getTransactionId().equals(o.getTransactionId()))
                .forEach(n -> byTx.putIfAbsent(n.getTransactionId(), n));
        return byTx;
    }

    private boolean isStrayRefunded(long orderId, String transactionId) {
        return refundMapper.selectCount(new LambdaQueryWrapper<OrderRefund>()
                .eq(OrderRefund::getOrderId, orderId).eq(OrderRefund::getKind, OrderRefund.KIND_STRAY)
                .eq(OrderRefund::getTransactionId, transactionId).in(OrderRefund::getStatus, ACTIVE)) > 0;
    }

    // ---------- 内部 ----------

    private Order lockOrder(String orderNo) {
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("FOR UPDATE"));
        if (o == null) throw BizException.notFound("订单不存在");
        return o;
    }

    private void requireSameChannel(String channel) {
        if (!payClient.channel().equals(channel))
            throw BizException.conflict("该笔款项通过「" + channel + "」收取，当前支付通道为「" + payClient.channel() + "」，无法原路退回");
    }

    private OrderRefund requireRefund(String refundNo) {
        OrderRefund r = refundMapper.selectOne(new LambdaQueryWrapper<OrderRefund>().eq(OrderRefund::getRefundNo, refundNo));
        if (r == null) throw new IllegalStateException("退款单不存在 " + refundNo);
        return r;
    }

    private OrderRefund insert(Order o, String kind, String channel, String transactionId, long totalFen,
                               BigDecimal amount, String reason, String operator) {
        OrderRefund r = new OrderRefund();
        r.setOrderId(o.getId());
        r.setOrderNo(o.getOrderNo());
        r.setKind(kind);
        r.setChannel(channel);
        r.setTransactionId(transactionId);
        r.setTotalFen(totalFen);
        r.setAmount(amount.setScale(2, RoundingMode.UNNECESSARY));
        r.setReason(reason.trim());
        r.setStatus(OrderRefund.Status.PROCESSING.name());
        r.setOperatorName(operator);
        for (int i = 0; ; i++) {
            r.setRefundNo("RF" + LocalDateTime.now().format(NO_FMT) + String.format("%08d", RANDOM.nextInt(100_000_000)));
            try {
                refundMapper.insert(r);
                return r;
            } catch (DuplicateKeyException e) {
                if (i >= 4) throw e;
            }
        }
    }
}
