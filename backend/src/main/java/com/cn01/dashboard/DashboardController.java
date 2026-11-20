package com.cn01.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cn01.common.Result;
import com.cn01.common.Status;
import com.cn01.news.News;
import com.cn01.news.NewsMapper;
import com.cn01.order.entity.Order;
import com.cn01.order.entity.OrderStatus;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.solution.entity.Solution;
import com.cn01.solution.mapper.SolutionMapper;
import com.cn01.user.AppUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final SolutionMapper solutionMapper;
    private final NewsMapper newsMapper;
    private final OrderMapper orderMapper;
    private final AppUserMapper userMapper;

    public record Summary(long solutionsOnSale, long solutionsTotal, long newsPublished, long users,
                          long ordersToday, long ordersPendingPay, long ordersToServe, BigDecimal paidAmountTotal) {}

    @GetMapping("/api/admin/dashboard")
    public Result<Summary> summary() {
        // 实收 = 已收款订单的金额 - 已退款金额（全额退款的订单贡献为 0）
        List<String> paidStatuses = List.of(OrderStatus.PAID.name(), OrderStatus.IN_SERVICE.name(), OrderStatus.COMPLETED.name(), OrderStatus.REFUNDED.name());
        List<Map<String, Object>> sum = orderMapper.selectMaps(new QueryWrapper<Order>()
                .select("COALESCE(SUM(total_amount - refunded_amount), 0) AS total").in("status", paidStatuses));
        Object total = sum.isEmpty() || sum.get(0) == null ? BigDecimal.ZERO : sum.get(0).get("total");
        return Result.ok(new Summary(
                solutionMapper.selectCount(new LambdaQueryWrapper<Solution>().eq(Solution::getStatus, Status.ON)),
                solutionMapper.selectCount(null),
                newsMapper.selectCount(new LambdaQueryWrapper<News>().eq(News::getStatus, Status.ON)),
                userMapper.selectCount(null),
                orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, LocalDate.now().atStartOfDay())),
                orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.PENDING_PAY.name())),
                orderMapper.selectCount(new LambdaQueryWrapper<Order>().in(Order::getStatus, OrderStatus.PAID.name(), OrderStatus.IN_SERVICE.name())),
                total instanceof BigDecimal b ? b : new BigDecimal(total.toString())));
    }
}
