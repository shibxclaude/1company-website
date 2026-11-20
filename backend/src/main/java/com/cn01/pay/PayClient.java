package com.cn01.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付通道。mock 与微信 Native 两种实现，由 app.pay.mode 切换。 */
public interface PayClient {
    String channel();

    boolean isMock();

    /** 下单并返回二维码链接（code_url） */
    String createNative(String orderNo, String description, BigDecimal amount, LocalDateTime expireAt);

    /** 关闭支付平台订单（失败不抛异常，只记日志） */
    void close(String orderNo);

    /** 主动查单 */
    QueryResult query(String orderNo);

    /**
     * 原路退款。refundNo 为我方退款单号（幂等键）；transactionId 为要退的那笔交易；totalFen 为该笔交易的原金额。
     * 网络超时等无法确定结果时返回 PROCESSING，由同步任务按 refundNo 查询。
     */
    RefundResult refund(String refundNo, String transactionId, long refundFen, long totalFen, String reason);

    /** 按我方退款单号查询退款结果 */
    RefundResult queryRefund(String refundNo);

    record PaidResult(String transactionId, long amountFen, String raw) {}

    record RefundResult(State state, String refundId, String message) {
        public enum State { SUCCESS, PROCESSING, FAILED }

        public static RefundResult success(String refundId) { return new RefundResult(State.SUCCESS, refundId, null); }
        public static RefundResult processing(String refundId) { return new RefundResult(State.PROCESSING, refundId, null); }
        public static RefundResult failed(String message) { return new RefundResult(State.FAILED, null, message); }
    }

    /** 查单结果：PAID 已支付；NOT_PAID 确定未支付（可以关单）；UNKNOWN 查询失败或用户正在支付（本轮不要动它） */
    record QueryResult(State state, PaidResult paid) {
        public enum State { PAID, NOT_PAID, UNKNOWN }

        public static QueryResult paid(PaidResult r) { return new QueryResult(State.PAID, r); }
        public static QueryResult notPaid() { return new QueryResult(State.NOT_PAID, null); }
        public static QueryResult unknown() { return new QueryResult(State.UNKNOWN, null); }
    }

    static long toFen(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
