package com.cn01;

import com.cn01.order.entity.Order;
import com.cn01.order.mapper.OrderLogMapper;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.order.entity.OrderLog;
import com.cn01.order.service.OrderCloseJob;
import com.cn01.pay.PaidHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class OrderPaymentIT extends IntegrationTestBase {
    @Autowired PaidHandler paidHandler;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderLogMapper logMapper;
    @Autowired OrderCloseJob closeJob;

    private Map<String, Object> orderReq(String phone) throws Exception {
        JsonNode d = ok(get("/api/portal/solutions/data-fusion"));
        Map<String, Object> m = new HashMap<>();
        m.put("slug", "data-fusion");
        m.put("versionId", d.get("versions").get(1).get("id").asLong());   // 专业版 368000
        m.put("quantity", 2);
        m.put("services", List.of(
                Map.of("serviceId", d.get("services").get(0).get("id").asLong(), "quantity", 3),
                Map.of("serviceId", d.get("services").get(2).get("id").asLong(), "quantity", 1)));
        m.put("contactName", "张三");
        m.put("contactPhone", phone);
        m.put("contactEmail", "zhangsan@example.com");
        m.put("companyName", "测试公司");
        m.put("agreed", true);
        return m;
    }

    private JsonNode createOrder(String phone) throws Exception {
        return ok(json(post("/api/portal/orders"), orderReq(phone)));
    }

    @Test
    void 订单号格式_金额以服务端价格计算() throws Exception {
        JsonNode d = ok(get("/api/portal/solutions/data-fusion"));
        BigDecimal v = new BigDecimal(d.get("versions").get(1).get("price").asText());
        BigDecimal s0 = new BigDecimal(d.get("services").get(0).get("price").asText());
        BigDecimal s2 = new BigDecimal(d.get("services").get(2).get("price").asText());
        Map<String, Object> req = orderReq("13900000001");
        req.put("totalAmount", "0.01");   // 前端传金额会被忽略
        JsonNode created = ok(json(post("/api/portal/orders"), req));

        assertThat(created.get("orderNo").asText()).matches("\\d{14}01\\d{6}");
        BigDecimal expected = v.multiply(BigDecimal.valueOf(2)).add(s0.multiply(BigDecimal.valueOf(3))).add(s2);
        assertThat(new BigDecimal(created.get("totalAmount").asText())).isEqualByComparingTo(expected);

        JsonNode view = ok(get("/api/portal/orders/" + created.get("orderNo").asText()).param("token", created.get("accessToken").asText()));
        assertThat(view.get("status").asText()).isEqualTo("PENDING_PAY");
        assertThat(view.get("items")).hasSize(3);
        assertThat(view.get("agreementVersion").asText()).isEqualTo("v1.0");
    }

    @Test
    void 没有token看不到订单_必须同意协议_参数校验() throws Exception {
        JsonNode created = createOrder("13900000002");
        String no = created.get("orderNo").asText();
        call(get("/api/portal/orders/" + no), 404);
        call(get("/api/portal/orders/" + no).param("token", "wrong"), 404);

        Map<String, Object> req = orderReq("13900000002");
        req.put("agreed", false);
        req.put("contactEmail", "bad");
        JsonNode r = call(json(post("/api/portal/orders"), req), 400);
        assertThat(r.get("msg").asText()).contains("产品购买服务协议").contains("邮箱格式不正确");

        Map<String, Object> req2 = orderReq("13900000002");
        req2.put("versionId", 999999);
        assertThat(call(json(post("/api/portal/orders"), req2), 400).get("msg").asText()).contains("版本已停售");
    }

    @Test
    void 完整链路_下单_二维码_模拟支付_重复回调幂等_后台推进服务进度() throws Exception {
        JsonNode created = createOrder("13900000003");
        String no = created.get("orderNo").asText();
        String token = created.get("accessToken").asText();

        JsonNode pay = ok(post("/api/portal/orders/" + no + "/pay").param("token", token));
        assertThat(pay.get("codeUrl").asText()).startsWith("weixin://wxpay/bizpayurl");
        assertThat(pay.get("mock").asBoolean()).isTrue();
        // 再次进入支付页复用同一个二维码
        assertThat(ok(post("/api/portal/orders/" + no + "/pay").param("token", token)).get("codeUrl").asText()).isEqualTo(pay.get("codeUrl").asText());

        assertThat(ok(post("/api/pay/mock/" + no + "/succeed").param("token", token)).asText()).isEqualTo("APPLIED");
        assertThat(ok(get("/api/portal/orders/" + no + "/status").param("token", token)).get("status").asText()).isEqualTo("PAID");

        // 同一笔交易的回调重复到达：不重复入账
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no));
        long fen = o.getTotalAmount().movePointRight(2).longValueExact();
        assertThat(paidHandler.onPaid("MOCK", no, o.getTransactionId(), "SUCCESS", fen, "{}")).isEqualTo(PaidHandler.Outcome.DUPLICATE);
        assertThat(paidHandler.onPaid("MOCK", no, o.getTransactionId(), "SUCCESS", fen, "{}")).isEqualTo(PaidHandler.Outcome.DUPLICATE);
        assertThat(logMapper.selectCount(new LambdaQueryWrapper<OrderLog>().eq(OrderLog::getOrderId, o.getId()).eq(OrderLog::getToStatus, "PAID"))).isEqualTo(1);
        // 另一笔交易又来付同一订单：拒绝（需人工退款）
        assertThat(paidHandler.onPaid("MOCK", no, "OTHER-TX", "SUCCESS", fen, "{}")).isEqualTo(PaidHandler.Outcome.REJECTED);
        // 已支付后不能再获取二维码、不能再模拟支付
        call(post("/api/portal/orders/" + no + "/pay").param("token", token), 409);
        call(post("/api/pay/mock/" + no + "/succeed").param("token", token), 409);

        // 后台：可见订单，推进状态
        String admin = adminToken();
        JsonNode list = ok(auth(get("/api/admin/orders").param("keyword", no), admin));
        assertThat(list.get("total").asInt()).isEqualTo(1);
        JsonNode detail = ok(auth(get("/api/admin/orders/" + no), admin));
        assertThat(detail.get("nextStatuses").toString()).contains("IN_SERVICE");

        call(auth(json(post("/api/admin/orders/" + no + "/progress"), Map.of("toStatus", "CANCELLED")), admin), 409);
        ok(auth(json(post("/api/admin/orders/" + no + "/progress"), Map.of("toStatus", "IN_SERVICE", "note", "已安排工程师远程部署")), admin));
        ok(auth(json(post("/api/admin/orders/" + no + "/progress"), Map.of("note", "完成数据对接")), admin));
        JsonNode done = ok(auth(json(post("/api/admin/orders/" + no + "/progress"), Map.of("toStatus", "COMPLETED", "note", "验收通过")), admin));
        assertThat(done.get("order").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("order").get("logs")).hasSize(5); // 提交、支付、服务中、备注、完成
    }

    @Test
    void 金额不符拒绝入账() throws Exception {
        JsonNode created = createOrder("13900000004");
        String no = created.get("orderNo").asText();
        assertThat(paidHandler.onPaid("MOCK", no, "TX-AMOUNT", "SUCCESS", 1, "{}")).isEqualTo(PaidHandler.Outcome.REJECTED);
        assertThat(orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no)).getStatus()).isEqualTo("PENDING_PAY");
    }

    @Test
    void 超时自动关单_关单后到账仍然入账() throws Exception {
        JsonNode created = createOrder("13900000005");
        String no = created.get("orderNo").asText();
        String token = created.get("accessToken").asText();
        ok(post("/api/portal/orders/" + no + "/pay").param("token", token));
        // 模拟 31 分钟前下的单
        orderMapper.update(new LambdaUpdateWrapper<Order>().eq(Order::getOrderNo, no).set(Order::getPayExpireAt, LocalDateTime.now().minusMinutes(1)));

        assertThat(closeJob.closeExpired(LocalDateTime.now())).isGreaterThanOrEqualTo(1);
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no));
        assertThat(o.getStatus()).isEqualTo("CANCELLED");
        assertThat(o.getCancelReason()).isEqualTo("超时未支付");
        call(post("/api/portal/orders/" + no + "/pay").param("token", token), 409);

        long fen = o.getTotalAmount().movePointRight(2).longValueExact();
        assertThat(paidHandler.onPaid("MOCK", no, "TX-LATE", "SUCCESS", fen, "{}")).isEqualTo(PaidHandler.Outcome.APPLIED);
        assertThat(orderMapper.selectById(o.getId()).getStatus()).isEqualTo("PAID");
    }

    @Test
    void 用户取消待支付订单() throws Exception {
        JsonNode created = createOrder("13900000006");
        String no = created.get("orderNo").asText();
        String token = created.get("accessToken").asText();
        ok(post("/api/portal/orders/" + no + "/cancel").param("token", token));
        assertThat(ok(get("/api/portal/orders/" + no).param("token", token)).get("status").asText()).isEqualTo("CANCELLED");
        call(post("/api/portal/orders/" + no + "/cancel").param("token", token), 409);
    }
}
