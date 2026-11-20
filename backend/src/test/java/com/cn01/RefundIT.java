package com.cn01;

import com.cn01.order.service.OrderRefundService;
import com.cn01.pay.PaidHandler;
import com.cn01.pay.PayClient;
import com.cn01.pay.PayClient.RefundResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 退款：后台发起、原路退回；支持部分退款；处理中占用额度；失败释放额度；异常收款（重复付款等）可整笔退回。 */
class RefundIT extends IntegrationTestBase {
    @MockitoSpyBean PayClient payClient;
    @Autowired PaidHandler paidHandler;
    @Autowired OrderRefundService refundService;

    @AfterEach
    void resetSpy() {
        reset(payClient);
    }

    record Paid(String orderNo, String token, BigDecimal total) {}

    private Paid paidOrder(String phone) throws Exception {
        JsonNode d = ok(get("/api/portal/solutions/iot"));
        JsonNode c = ok(json(post("/api/portal/orders"), Map.of("slug", "iot",
                "versionId", d.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "退款测试", "contactPhone", phone, "contactEmail", "r@example.com", "agreed", true)));
        String no = c.get("orderNo").asText(), token = c.get("accessToken").asText();
        ok(post("/api/portal/orders/" + no + "/pay").header("X-Order-Token", token));
        ok(post("/api/pay/mock/" + no + "/succeed").header("X-Order-Token", token));
        return new Paid(no, token, new BigDecimal(c.get("totalAmount").asText()));
    }

    private JsonNode refund(String admin, String no, Object amount, int expectHttp) throws Exception {
        return call(auth(json(post("/api/admin/orders/" + no + "/refunds"), Map.of("amount", amount, "reason", "客户申请")), admin), expectHttp);
    }

    private JsonNode detail(String admin, String no) throws Exception {
        return ok(auth(get("/api/admin/orders/" + no), admin));
    }

    private BigDecimal paidTotal(String admin) throws Exception {
        return new BigDecimal(ok(auth(get("/api/admin/dashboard"), admin)).get("paidAmountTotal").asText());
    }

    @Test
    void 全额退款_订单变为已退款_客户可见_收入统计扣除() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000001");
        BigDecimal before = paidTotal(admin);

        JsonNode d = refund(admin, p.orderNo(), p.total(), 200).get("data");
        assertThat(d.get("order").get("status").asText()).isEqualTo("REFUNDED");
        assertThat(new BigDecimal(d.get("order").get("refundedAmount").asText())).isEqualByComparingTo(p.total());
        assertThat(d.get("refunds").get(0).get("status").asText()).isEqualTo("SUCCESS");
        assertThat(d.get("refunds").get(0).get("operatorName").asText()).isEqualTo("admin");
        assertThat(new BigDecimal(d.get("refundableAmount").asText())).isEqualByComparingTo("0");

        JsonNode portal = ok(get("/api/portal/orders/" + p.orderNo()).header("X-Order-Token", p.token()));
        assertThat(portal.get("statusLabel").asText()).isEqualTo("已退款");
        assertThat(new BigDecimal(portal.get("refundedAmount").asText())).isEqualByComparingTo(p.total());

        assertThat(before.subtract(paidTotal(admin))).isEqualByComparingTo(p.total());
    }

    @Test
    void 部分退款_状态不变_累计不能超过实付() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000002");
        JsonNode d = refund(admin, p.orderNo(), "100", 200).get("data");   // 后台输入框提交的是不带小数的字符串
        assertThat(d.get("order").get("status").asText()).isEqualTo("PAID");
        assertThat(d.get("order").get("logs").findValuesAsText("note")).contains("发起退款 ¥100.00，原因：客户申请");
        assertThat(new BigDecimal(d.get("refundableAmount").asText())).isEqualByComparingTo(p.total().subtract(new BigDecimal("100")));

        BigDecimal rest = p.total().subtract(new BigDecimal("100"));
        assertThat(refund(admin, p.orderNo(), rest.add(new BigDecimal("0.01")), 400).get("msg").asText()).contains("可退");
        assertThat(refund(admin, p.orderNo(), rest, 200).get("data").get("order").get("status").asText()).isEqualTo("REFUNDED");
    }

    @Test
    void 待支付和已完成的订单不能退款_金额必须为正() throws Exception {
        String admin = adminToken();
        JsonNode dd = ok(get("/api/portal/solutions/iot"));
        String pending = ok(json(post("/api/portal/orders"), Map.of("slug", "iot",
                "versionId", dd.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "x", "contactPhone", "13400000003", "contactEmail", "x@example.com", "agreed", true))).get("orderNo").asText();
        refund(admin, pending, "1.00", 409);

        Paid p = paidOrder("13400000004");
        refund(admin, p.orderNo(), "0", 400);
        ok(auth(json(post("/api/admin/orders/" + p.orderNo() + "/progress"), Map.of("toStatus", "COMPLETED")), admin));
        refund(admin, p.orderNo(), "1.00", 409);
    }

    @Test
    void 退款处理中占用额度_查询到成功后入账_重复确认只入一次() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000005");
        doReturn(RefundResult.processing(null)).when(payClient).refund(anyString(), anyString(), anyLong(), anyLong(), anyString());

        JsonNode d = refund(admin, p.orderNo(), p.total(), 200).get("data");
        assertThat(d.get("order").get("status").asText()).isEqualTo("PAID");
        assertThat(d.get("refunds").get(0).get("status").asText()).isEqualTo("PROCESSING");
        assertThat(new BigDecimal(d.get("refundableAmount").asText())).isEqualByComparingTo("0");
        refund(admin, p.orderNo(), "1.00", 400);   // 处理中的金额已占用额度

        String refundNo = d.get("refunds").get(0).get("refundNo").asText();
        doReturn(RefundResult.success("WX-REFUND-1")).when(payClient).queryRefund(refundNo);
        assertThat(refundService.sync(LocalDateTime.now().plusMinutes(5))).isEqualTo(1);
        refundService.applySuccess(refundNo, "WX-REFUND-1");   // 回调与查单都确认：只能入账一次

        JsonNode after = detail(admin, p.orderNo());
        assertThat(after.get("order").get("status").asText()).isEqualTo("REFUNDED");
        assertThat(new BigDecimal(after.get("order").get("refundedAmount").asText())).isEqualByComparingTo(p.total());
    }

    @Test
    void 退款处理中订单被改为已完成_全额到账后仍变为已退款() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000009");
        doReturn(RefundResult.processing(null)).when(payClient).refund(anyString(), anyString(), anyLong(), anyLong(), anyString());
        String refundNo = refund(admin, p.orderNo(), p.total(), 200).get("data").get("refunds").get(0).get("refundNo").asText();

        ok(auth(json(post("/api/admin/orders/" + p.orderNo() + "/progress"), Map.of("toStatus", "COMPLETED")), admin));
        refundService.applySuccess(refundNo, "WX-REFUND-C");

        JsonNode after = detail(admin, p.orderNo());
        assertThat(after.get("order").get("status").asText()).isEqualTo("REFUNDED");
        assertThat(new BigDecimal(after.get("order").get("refundedAmount").asText())).isEqualByComparingTo(p.total());
    }

    @Test
    void 支付平台拒绝退款_标记失败并释放额度() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000006");
        doReturn(RefundResult.failed("商户可用余额不足")).when(payClient).refund(anyString(), anyString(), anyLong(), anyLong(), anyString());
        JsonNode d = refund(admin, p.orderNo(), p.total(), 200).get("data");
        assertThat(d.get("refunds").get(0).get("status").asText()).isEqualTo("FAILED");
        assertThat(d.get("refunds").get(0).get("failReason").asText()).contains("余额不足");
        assertThat(new BigDecimal(d.get("refundableAmount").asText())).isEqualByComparingTo(p.total());

        reset(payClient);
        assertThat(refund(admin, p.orderNo(), p.total(), 200).get("data").get("order").get("status").asText()).isEqualTo("REFUNDED");
    }

    @Test
    void 重复付款的异常收款_可以整笔退回且只能退一次() throws Exception {
        String admin = adminToken();
        Paid p = paidOrder("13400000007");
        long fen = PayClient.toFen(p.total());
        assertThat(paidHandler.onPaid("MOCK", p.orderNo(), "TX-DUP-7", "SUCCESS", fen, "{}")).isEqualTo(PaidHandler.Outcome.REJECTED);

        JsonNode d = detail(admin, p.orderNo());
        assertThat(d.get("abnormalPayments").get(0).get("transactionId").asText()).isEqualTo("TX-DUP-7");
        assertThat(d.get("abnormalPayments").get(0).get("refunded").asBoolean()).isFalse();

        JsonNode r = ok(auth(json(post("/api/admin/orders/" + p.orderNo() + "/refunds/stray"),
                Map.of("transactionId", "TX-DUP-7", "reason", "重复付款")), admin));
        assertThat(r.get("order").get("status").asText()).isEqualTo("PAID");   // 订单本身不受影响
        assertThat(new BigDecimal(r.get("order").get("refundedAmount").asText())).isEqualByComparingTo("0");
        assertThat(r.get("abnormalPayments").get(0).get("refunded").asBoolean()).isTrue();
        assertThat(r.get("refunds").get(0).get("kind").asText()).isEqualTo("STRAY");
        verify(payClient).refund(anyString(), eq("TX-DUP-7"), eq(fen), eq(fen), anyString());

        call(auth(json(post("/api/admin/orders/" + p.orderNo() + "/refunds/stray"),
                Map.of("transactionId", "TX-DUP-7", "reason", "重复付款")), admin), 409);
        call(auth(json(post("/api/admin/orders/" + p.orderNo() + "/refunds/stray"),
                Map.of("transactionId", "NOT-EXIST", "reason", "x")), admin), 404);
    }
}
