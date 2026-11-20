package com.cn01;

import com.cn01.common.RichText;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.service.OrderNoGenerator;
import com.cn01.pay.PayClient;
import com.cn01.pay.WechatPayClient;
import com.wechat.pay.java.service.refund.model.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitTests {
    @Test
    void 订单号_日期加业务码加随机数() {
        String no = OrderNoGenerator.next("01", LocalDateTime.of(2026, 11, 20, 9, 5, 7));
        assertThat(no).hasSize(22).startsWith("2026112009050701").matches("\\d{22}");
    }

    @Test
    void 金额转分_精确且拒绝超过两位小数() {
        assertThat(PayClient.toFen(new BigDecimal("368000.00"))).isEqualTo(36800000L);
        assertThat(PayClient.toFen(new BigDecimal("0.1"))).isEqualTo(10L);
        assertThatThrownBy(() -> PayClient.toFen(new BigDecimal("1.005"))).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void 后台状态流转规则() {
        assertThat(OrderStatus.PAID.adminCanMoveTo(OrderStatus.IN_SERVICE)).isTrue();
        assertThat(OrderStatus.IN_SERVICE.adminCanMoveTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.PAID.adminCanMoveTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.PENDING_PAY.adminCanMoveTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.COMPLETED.adminCanMoveTo(OrderStatus.IN_SERVICE)).isFalse();
    }

    @Test
    void 退款规则_已完成不能发起退款_但在途退款全额到账后可变为已退款() {
        assertThat(OrderStatus.COMPLETED.refundable()).isFalse();
        assertThat(OrderStatus.COMPLETED.settlesToRefunded()).isTrue();
        assertThat(OrderStatus.PAID.settlesToRefunded()).isTrue();
        assertThat(OrderStatus.IN_SERVICE.settlesToRefunded()).isTrue();
        assertThat(OrderStatus.REFUNDED.settlesToRefunded()).isFalse();
        assertThat(OrderStatus.CANCELLED.settlesToRefunded()).isFalse();
        assertThat(OrderStatus.PENDING_PAY.settlesToRefunded()).isFalse();
    }

    @Test
    void 富文本清洗() {
        String out = RichText.clean("<h3 style=\"color:red\">标题</h3><a href=\"javascript:alert(1)\">x</a><iframe src=\"//evil\"></iframe><img src=\"x\" onerror=\"alert(1)\">");
        assertThat(out).contains("<h3 style=\"color:red\">标题</h3>").doesNotContain("javascript").doesNotContain("iframe").doesNotContain("onerror");
    }

    @Test
    void 富文本样式只保留排版相关属性_去掉定位和外链背景() {
        String out = RichText.clean("<p style=\"position:fixed; top:0; z-index:9999; color:#333; text-align:center; background:url(https://evil/x.png)\">x</p>"
                + "<div style=\"width:expression(alert(1))\">y</div>");
        assertThat(out).contains("<p style=\"color:#333; text-align:center\">x</p>")
                .doesNotContain("position").doesNotContain("z-index").doesNotContain("url(").doesNotContain("expression");
    }

    @Test
    void 微信退款状态映射() {
        assertThat(WechatPayClient.toRefundResult(Status.SUCCESS, "R1")).isEqualTo(PayClient.RefundResult.success("R1"));
        assertThat(WechatPayClient.toRefundResult(Status.PROCESSING, "R1").state()).isEqualTo(PayClient.RefundResult.State.PROCESSING);
        assertThat(WechatPayClient.toRefundResult(Status.CLOSED, "R1").state()).isEqualTo(PayClient.RefundResult.State.FAILED);
        assertThat(WechatPayClient.toRefundResult(Status.ABNORMAL, "R1").message()).contains("商户平台");
    }
}
