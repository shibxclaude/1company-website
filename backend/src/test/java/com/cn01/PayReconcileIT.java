package com.cn01;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn01.order.entity.Order;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.order.service.OrderCloseJob;
import com.cn01.pay.PayClient;
import com.cn01.pay.PayClient.QueryResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 关单前查单失败不能关；已关闭但其实付了款的订单由对账任务找回。 */
class PayReconcileIT extends IntegrationTestBase {
    @MockitoSpyBean PayClient payClient;
    @Autowired OrderMapper orderMapper;
    @Autowired OrderCloseJob closeJob;

    private String expiredOrderWithQr(String phone) throws Exception {
        JsonNode d = ok(get("/api/portal/solutions/iot"));
        JsonNode created = ok(json(post("/api/portal/orders"), Map.of("slug", "iot",
                "versionId", d.get("versions").get(0).get("id").asLong(), "quantity", 1,
                "contactName", "钱七", "contactPhone", phone, "contactEmail", "q@example.com", "agreed", true)));
        String no = created.get("orderNo").asText();
        ok(post("/api/portal/orders/" + no + "/pay").param("token", created.get("accessToken").asText()));
        orderMapper.update(new LambdaUpdateWrapper<Order>().eq(Order::getOrderNo, no).set(Order::getPayExpireAt, LocalDateTime.now().minusMinutes(1)));
        return no;
    }

    private Order load(String no) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, no));
    }

    @Test
    void 查单结果不确定时_本轮不关单也不调关单() throws Exception {
        String no = expiredOrderWithQr("13500000001");
        doReturn(QueryResult.unknown()).when(payClient).query(eq(no));
        closeJob.closeExpired(LocalDateTime.now());
        assertThat(load(no).getStatus()).isEqualTo("PENDING_PAY");
        verify(payClient, never()).close(eq(no));
    }

    @Test
    void 对账_已关闭订单在支付平台实际已付_恢复为已支付() throws Exception {
        String no = expiredOrderWithQr("13500000002");
        closeJob.closeExpired(LocalDateTime.now());
        Order o = load(no);
        assertThat(o.getStatus()).isEqualTo("CANCELLED");

        long fen = PayClient.toFen(o.getTotalAmount());
        doReturn(QueryResult.paid(new PayClient.PaidResult("TX-RECON", fen, "{}"))).when(payClient).query(eq(no));
        assertThat(closeJob.reconcile(LocalDateTime.now())).isGreaterThanOrEqualTo(1);
        assertThat(load(no).getStatus()).isEqualTo("PAID");
        assertThat(load(no).getTransactionId()).isEqualTo("TX-RECON");
    }
}
