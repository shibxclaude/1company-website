package com.cn01.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** 支付通道。mock 与微信 Native 两种实现，由 app.pay.mode 切换。 */
public interface PayClient {
    String channel();

    boolean isMock();

    /** 下单并返回二维码链接（code_url） */
    String createNative(String orderNo, String description, BigDecimal amount, LocalDateTime expireAt);

    /** 关闭支付平台订单（失败不抛异常，只记日志） */
    void close(String orderNo);

    /** 主动查单；已支付时返回交易结果 */
    Optional<PaidResult> query(String orderNo);

    record PaidResult(String transactionId, long amountFen, String raw) {}

    static long toFen(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
