package com.cn01.notify;

import com.cn01.config.AppProperties;
import com.cn01.order.entity.Order;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 支付成功通知：短信 + 邮件（客户），邮件（管理员）。异步执行，失败只记日志，不影响入账。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final OrderMapper orderMapper;
    private final SmsSender smsSender;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    @Async
    public void orderPaid(long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) return;
        String link = props.getSiteUrl() + "/user/orders/" + o.getOrderNo();
        try {
            smsSender.sendText(o.getContactPhone(), String.format("【一零科技】您的订单 %s（%s）已支付成功，金额 ¥%s。我们将尽快与您联系安排交付。",
                    o.getOrderNo(), o.getSolutionName(), o.getTotalAmount().toPlainString()));
        } catch (Exception e) {
            log.warn("支付成功短信发送失败 orderNo={}：{}", o.getOrderNo(), e.getMessage());
        }
        sendMail(o.getContactEmail(), "【一零科技】订单支付成功 " + o.getOrderNo(), String.join("\n",
                o.getContactName() + "，您好：",
                "",
                "您的订单已支付成功，我们将在 1 个工作日内与您联系，安排交付与服务。",
                "订单号：" + o.getOrderNo(),
                "解决方案：" + o.getSolutionName(),
                "支付金额：¥" + o.getTotalAmount().toPlainString(),
                "",
                "登录用户中心可随时查看订单与服务进度：" + link,
                "如有疑问请致电 136 9928 5133。",
                "",
                "北京一零科技有限公司"));
        String admin = props.getNotify().getAdminEmail();
        if (StringUtils.hasText(admin)) {
            sendMail(admin, "【新订单已支付】" + o.getOrderNo() + " " + o.getSolutionName(), String.join("\n",
                    "订单号：" + o.getOrderNo(), "解决方案：" + o.getSolutionName(), "金额：¥" + o.getTotalAmount().toPlainString(),
                    "联系人：" + o.getContactName() + " " + o.getContactPhone() + " " + o.getContactEmail(),
                    "企业：" + (o.getCompanyName() == null ? "—" : o.getCompanyName()),
                    "请在管理后台跟进服务进度。"));
        }
    }

    private void sendMail(String to, String subject, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        String from = props.getNotify().getMailFrom();
        if (sender == null || !StringUtils.hasText(from)) {
            log.info("[邮件-未配置] 收件人 {} 主题 {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            sender.send(m);
        } catch (Exception e) {
            log.warn("邮件发送失败 to={}：{}", to, e.getMessage());
        }
    }
}
