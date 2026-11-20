package com.cn01.pay;

import com.cn01.common.BizException;
import com.cn01.common.ErrorCode;
import com.cn01.config.AppProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.WechatPayException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 微信支付 Native（APIv3，官方 SDK wechatpay-java）。
 * RSAAutoCertificateConfig 会自动下载并定时更新平台证书，回调验签与解密由 NotificationParser 完成。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.pay", name = "mode", havingValue = "wechat")
public class WechatPayClient implements PayClient {
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AppProperties.Wechat cfg;
    private final String notifyUrl;
    private final NativePayService service;
    private final RefundService refundService;
    private final String refundNotifyUrl;
    private final NotificationParser parser;

    public WechatPayClient(AppProperties props) {
        this.cfg = props.getPay().getWechat();
        this.notifyUrl = props.getPay().getNotifyUrl();
        RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(cfg.getMchId())
                .privateKeyFromPath(cfg.getPrivateKeyPath())
                .merchantSerialNumber(cfg.getMerchantSerialNumber())
                .apiV3Key(cfg.getApiV3Key())
                .build();
        this.service = new NativePayService.Builder().config(config).build();
        this.refundService = new RefundService.Builder().config(config).build();
        this.refundNotifyUrl = props.getPay().getRefundNotifyUrl();
        this.parser = new NotificationParser(config);
    }

    @Override
    public String channel() { return "WECHAT_NATIVE"; }

    @Override
    public boolean isMock() { return false; }

    @Override
    public String createNative(String orderNo, String description, BigDecimal amount, LocalDateTime expireAt) {
        PrepayRequest req = new PrepayRequest();
        Amount a = new Amount();
        a.setTotal(Math.toIntExact(PayClient.toFen(amount)));
        a.setCurrency("CNY");
        req.setAmount(a);
        req.setAppid(cfg.getAppId());
        req.setMchid(cfg.getMchId());
        req.setDescription(description.length() > 120 ? description.substring(0, 120) : description);
        req.setNotifyUrl(notifyUrl);
        req.setOutTradeNo(orderNo);
        req.setTimeExpire(expireAt.atZone(ZoneId.of("Asia/Shanghai")).format(RFC3339));
        try {
            return service.prepay(req).getCodeUrl();
        } catch (WechatPayException e) {
            log.error("微信 Native 下单失败 orderNo={}", orderNo, e);
            throw new BizException(ErrorCode.PAY_ERROR, null);
        }
    }

    @Override
    public void close(String orderNo) {
        try {
            CloseOrderRequest req = new CloseOrderRequest();
            req.setMchid(cfg.getMchId());
            req.setOutTradeNo(orderNo);
            service.closeOrder(req);
        } catch (WechatPayException e) {
            log.warn("微信关单失败 orderNo={}：{}", orderNo, e.getMessage());
        }
    }

    @Override
    public QueryResult query(String orderNo) {
        try {
            QueryOrderByOutTradeNoRequest req = new QueryOrderByOutTradeNoRequest();
            req.setMchid(cfg.getMchId());
            req.setOutTradeNo(orderNo);
            Transaction t = service.queryOrderByOutTradeNo(req);
            var state = t.getTradeState();
            if (state == Transaction.TradeStateEnum.SUCCESS) {
                return QueryResult.paid(new PaidResult(t.getTransactionId(), t.getAmount().getTotal(), t.toString()));
            }
            if (state == Transaction.TradeStateEnum.NOTPAY || state == Transaction.TradeStateEnum.CLOSED
                    || state == Transaction.TradeStateEnum.REVOKED || state == Transaction.TradeStateEnum.PAYERROR) {
                return QueryResult.notPaid();
            }
            // USERPAYING（用户正在输密码）、REFUND 等：不能当作未支付处理
            log.warn("微信查单状态需观察 orderNo={} state={}", orderNo, state);
            return QueryResult.unknown();
        } catch (ServiceException e) {
            if ("ORDER_NOT_EXIST".equals(e.getErrorCode())) return QueryResult.notPaid();
            log.warn("微信查单失败 orderNo={}：{}", orderNo, e.getMessage());
        } catch (WechatPayException e) {
            log.warn("微信查单失败 orderNo={}：{}", orderNo, e.getMessage());
        }
        return QueryResult.unknown();
    }

    /** 验签 + 解密回调；验签失败抛 ValidationException */
    public Transaction parseNotify(String serial, String nonce, String signature, String timestamp, String signType, String body) {
        RequestParam param = new RequestParam.Builder()
                .serialNumber(serial).nonce(nonce).signature(signature).timestamp(timestamp).signType(signType).body(body)
                .build();
        return parser.parse(param, Transaction.class);
    }

    @Override
    public RefundResult refund(String refundNo, String transactionId, long refundFen, long totalFen, String reason) {
        CreateRequest req = new CreateRequest();
        req.setOutRefundNo(refundNo);
        req.setTransactionId(transactionId);
        req.setReason(reason.length() > 80 ? reason.substring(0, 80) : reason);
        req.setNotifyUrl(refundNotifyUrl);
        AmountReq a = new AmountReq();
        a.setRefund(refundFen);
        a.setTotal(totalFen);
        a.setCurrency("CNY");
        req.setAmount(a);
        try {
            Refund r = refundService.create(req);
            return toRefundResult(r.getStatus(), r.getRefundId());
        } catch (ServiceException e) {
            // 系统繁忙、频率限制：微信要求用同一退款单号重试，交给同步任务查询；其余错误码是明确的拒绝
            if ("SYSTEM_ERROR".equals(e.getErrorCode()) || "FREQUENCY_LIMITED".equals(e.getErrorCode())) {
                log.warn("微信退款暂时失败，等待同步 refundNo={}：{}", refundNo, e.getErrorMessage());
                return RefundResult.processing(null);
            }
            log.warn("微信退款被拒 refundNo={} code={}：{}", refundNo, e.getErrorCode(), e.getErrorMessage());
            return RefundResult.failed(e.getErrorMessage() == null ? e.getErrorCode() : e.getErrorMessage());
        } catch (WechatPayException e) {
            log.warn("微信退款结果未知（网络等），等待同步 refundNo={}：{}", refundNo, e.getMessage());
            return RefundResult.processing(null);
        }
    }

    @Override
    public RefundResult queryRefund(String refundNo) {
        try {
            QueryByOutRefundNoRequest req = new QueryByOutRefundNoRequest();
            req.setOutRefundNo(refundNo);
            Refund r = refundService.queryByOutRefundNo(req);
            return toRefundResult(r.getStatus(), r.getRefundId());
        } catch (ServiceException e) {
            if ("RESOURCE_NOT_EXISTS".equals(e.getErrorCode())) return RefundResult.failed("支付平台没有这笔退款（退款请求未送达），可重新发起");
            log.warn("微信退款查询失败 refundNo={}：{}", refundNo, e.getErrorMessage());
        } catch (WechatPayException e) {
            log.warn("微信退款查询失败 refundNo={}：{}", refundNo, e.getMessage());
        }
        return RefundResult.processing(null);
    }

    public static RefundResult toRefundResult(Status status, String refundId) {
        if (status == null) return RefundResult.processing(refundId);
        return switch (status) {
            case SUCCESS -> RefundResult.success(refundId);
            case PROCESSING -> RefundResult.processing(refundId);
            case CLOSED -> RefundResult.failed("退款已关闭");
            case ABNORMAL -> RefundResult.failed("退款异常（如用户银行卡已注销），需在微信商户平台手动处理");
        };
    }

    /** 验签 + 解密退款回调；验签失败抛 ValidationException */
    public RefundNotification parseRefundNotify(String serial, String nonce, String signature, String timestamp, String signType, String body) {
        RequestParam param = new RequestParam.Builder()
                .serialNumber(serial).nonce(nonce).signature(signature).timestamp(timestamp).signType(signType).body(body)
                .build();
        return parser.parse(param, RefundNotification.class);
    }
}
