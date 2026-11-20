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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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
    public Optional<PaidResult> query(String orderNo) {
        try {
            QueryOrderByOutTradeNoRequest req = new QueryOrderByOutTradeNoRequest();
            req.setMchid(cfg.getMchId());
            req.setOutTradeNo(orderNo);
            Transaction t = service.queryOrderByOutTradeNo(req);
            if (t.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                return Optional.of(new PaidResult(t.getTransactionId(), t.getAmount().getTotal(), t.toString()));
            }
        } catch (ServiceException e) {
            if (!"ORDER_NOT_EXIST".equals(e.getErrorCode())) log.warn("微信查单失败 orderNo={}：{}", orderNo, e.getMessage());
        } catch (WechatPayException e) {
            log.warn("微信查单失败 orderNo={}：{}", orderNo, e.getMessage());
        }
        return Optional.empty();
    }

    /** 验签 + 解密回调；验签失败抛 ValidationException */
    public Transaction parseNotify(String serial, String nonce, String signature, String timestamp, String signType, String body) {
        RequestParam param = new RequestParam.Builder()
                .serialNumber(serial).nonce(nonce).signature(signature).timestamp(timestamp).signType(signType).body(body)
                .build();
        return parser.parse(param, Transaction.class);
    }
}
