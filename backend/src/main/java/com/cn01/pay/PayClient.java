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

    record PaidResult(String transactionId, long amountFen, String raw) {}

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
