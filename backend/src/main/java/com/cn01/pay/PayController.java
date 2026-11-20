package com.cn01.pay;

import com.cn01.common.BizException;
import com.cn01.common.Result;
import com.cn01.config.AppProperties;
import com.cn01.order.dto.OrderDtos.PayInfo;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.service.OrderRefundService;
import com.cn01.order.service.OrderService;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PayController {
    private final PayService payService;
    private final PaidHandler paidHandler;
    private final OrderService orderService;
    private final OrderRefundService refundService;
    private final AppProperties props;
    private final ObjectProvider<WechatPayClient> wechat;

    /** 获取支付二维码（游客带 token，登录用户带 Authorization） */
    @PostMapping("/api/portal/orders/{orderNo}/pay")
    public Result<PayInfo> pay(@PathVariable String orderNo, @RequestHeader(value = "X-Order-Token", required = false) String headerToken, @RequestParam(required = false) String token) {
        return Result.ok(payService.prepare(orderNo, OrderService.pickToken(headerToken, token)));
    }

    /** 微信支付结果通知（APIv3）：验签、解密、幂等入账。成功返回 200，失败返回非 2xx 让微信重试。 */
    @PostMapping("/api/pay/notify/wechat")
    public ResponseEntity<Map<String, String>> wechatNotify(@RequestBody String body, HttpServletRequest req) {
        WechatPayClient client = wechat.getIfAvailable();
        if (client == null) return ResponseEntity.status(404).body(Map.of("code", "FAIL", "message", "未启用微信支付"));
        Transaction t;
        try {
            t = client.parseNotify(req.getHeader("Wechatpay-Serial"), req.getHeader("Wechatpay-Nonce"),
                    req.getHeader("Wechatpay-Signature"), req.getHeader("Wechatpay-Timestamp"),
                    req.getHeader("Wechatpay-Signature-Type"), body);
        } catch (ValidationException e) {
            log.warn("微信回调验签失败：{}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("code", "FAIL", "message", "签名验证失败"));
        }
        try {
            paidHandler.onPaid(client.channel(), t.getOutTradeNo(), t.getTransactionId(),
                    t.getTradeState() == null ? "" : t.getTradeState().name(),
                    t.getAmount() == null || t.getAmount().getTotal() == null ? -1 : t.getAmount().getTotal(), t.toString());
        } catch (Exception e) {
            log.error("处理微信回调失败 orderNo={}", t.getOutTradeNo(), e);
            return ResponseEntity.status(500).body(Map.of("code", "FAIL", "message", "处理失败"));
        }
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
    }

    /** 微信退款结果通知（APIv3）：验签、解密、幂等确认 */
    @PostMapping("/api/pay/notify/wechat-refund")
    public ResponseEntity<Map<String, String>> wechatRefundNotify(@RequestBody String body, HttpServletRequest req) {
        WechatPayClient client = wechat.getIfAvailable();
        if (client == null) return ResponseEntity.status(404).body(Map.of("code", "FAIL", "message", "未启用微信支付"));
        RefundNotification n;
        try {
            n = client.parseRefundNotify(req.getHeader("Wechatpay-Serial"), req.getHeader("Wechatpay-Nonce"),
                    req.getHeader("Wechatpay-Signature"), req.getHeader("Wechatpay-Timestamp"),
                    req.getHeader("Wechatpay-Signature-Type"), body);
        } catch (ValidationException e) {
            log.warn("微信退款回调验签失败：{}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("code", "FAIL", "message", "签名验证失败"));
        }
        try {
            refundService.apply(n.getOutRefundNo(), WechatPayClient.toRefundResult(n.getRefundStatus(), n.getRefundId()));
        } catch (Exception e) {
            log.error("处理微信退款回调失败 refundNo={}", n.getOutRefundNo(), e);
            return ResponseEntity.status(500).body(Map.of("code", "FAIL", "message", "处理失败"));
        }
        return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
    }

    /** 模拟支付成功（仅 app.pay.mode=mock 时可用），与真实回调走同一个入账逻辑 */
    @PostMapping("/api/pay/mock/{orderNo}/succeed")
    public Result<String> mockSucceed(@PathVariable String orderNo, @RequestHeader(value = "X-Order-Token", required = false) String headerToken, @RequestParam(required = false) String token) {
        if (!props.getPay().isMock()) throw BizException.notFound("接口不存在");
        Order o = orderService.requireAccessible(orderNo, OrderService.pickToken(headerToken, token));
        if (o.statusEnum() != OrderStatus.PENDING_PAY) throw BizException.conflict("订单不是待支付状态");
        if (o.getCodeUrl() == null) throw BizException.conflict("请先获取支付二维码");
        String tx = "MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        PaidHandler.Outcome r = paidHandler.onPaid("MOCK", orderNo, tx, "SUCCESS", PayClient.toFen(o.getTotalAmount()), "{\"mock\":true}");
        return Result.ok(r.name());
    }
}
