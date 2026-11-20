package com.cn01.pay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟支付（开发 / 演示）。二维码内容是一个模拟链接；支付页上的"模拟支付成功"按钮调用
 * /api/pay/mock/{orderNo}/succeed，走与真实回调完全相同的入账逻辑。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.pay", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockPayClient implements PayClient {
    @Override
    public String channel() { return "MOCK"; }

    @Override
    public boolean isMock() { return true; }

    @Override
    public String createNative(String orderNo, String description, BigDecimal amount, LocalDateTime expireAt) {
        log.info("[模拟支付] 下单 {} {} ¥{}", orderNo, description, amount);
        return "weixin://wxpay/bizpayurl?pr=MOCK" + orderNo;
    }

    @Override
    public void close(String orderNo) {
        log.info("[模拟支付] 关单 {}", orderNo);
    }

    @Override
    public QueryResult query(String orderNo) {
        return QueryResult.notPaid();
    }
}
