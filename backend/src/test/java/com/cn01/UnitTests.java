package com.cn01;

import com.cn01.common.RichText;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.service.OrderNoGenerator;
import com.cn01.pay.PayClient;
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
    void 富文本清洗() {
        String out = RichText.clean("<h3 style=\"color:red\">标题</h3><a href=\"javascript:alert(1)\">x</a><iframe src=\"//evil\"></iframe><img src=\"x\" onerror=\"alert(1)\">");
        assertThat(out).contains("<h3 style=\"color:red\">标题</h3>").doesNotContain("javascript").doesNotContain("iframe").doesNotContain("onerror");
    }
}
