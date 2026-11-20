package com.cn01.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn01.auth.AuthContext;
import com.cn01.auth.AuthPrincipal;
import com.cn01.auth.RateLimiter;
import com.cn01.common.BizException;
import com.cn01.common.PageResult;
import com.cn01.common.Status;
import com.cn01.config.AppProperties;
import com.cn01.order.dto.OrderDtos.*;
import com.cn01.order.entity.*;
import com.cn01.order.mapper.OrderItemMapper;
import com.cn01.order.mapper.OrderMapper;
import com.cn01.pay.PayClient;
import com.cn01.solution.entity.*;
import com.cn01.solution.mapper.*;
import com.cn01.user.AppUser;
import com.cn01.user.AppUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** 前台下单、查看、取消。价格一律以数据库为准，前端传来的金额不参与计算。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final SolutionMapper solutionMapper;
    private final SolutionCategoryMapper categoryMapper;
    private final SolutionVersionMapper versionMapper;
    private final SolutionServiceMapper serviceMapper;
    private final AppUserMapper userMapper;
    private final OrderAssembler assembler;
    private final RateLimiter rateLimiter;
    private final AppProperties props;
    private final PayClient payClient;

    @Transactional
    public Created create(CreateReq req, String ip) {
        rateLimiter.hit("order-create:" + ip, props.getOrder().getCreateLimitPerHour(), Duration.ofHours(1), "下单过于频繁，请稍后再试");

        Solution s = solutionMapper.selectOne(new LambdaQueryWrapper<Solution>().eq(Solution::getSlug, req.slug()).eq(Solution::getStatus, Status.ON));
        SolutionCategory cat = s == null ? null : categoryMapper.selectById(s.getCategoryId());
        if (s == null || cat == null || cat.getStatus() != Status.ON) throw BizException.badRequest("该解决方案已下架，请刷新页面");
        SolutionVersion v = versionMapper.selectById(req.versionId());
        if (v == null || !v.getSolutionId().equals(s.getId()) || v.getStatus() != Status.ON) throw BizException.badRequest("所选版本已停售，请刷新页面");

        List<OrderItem> items = new ArrayList<>();
        BillingType bt = BillingType.valueOf(v.getBillingType());
        items.add(item(OrderItem.VERSION, v.getId(), v.getName(), bt.name(), bt.unit(), v.getPrice(), req.quantity()));

        Set<Long> seen = new HashSet<>();
        for (ServiceSel sel : Optional.ofNullable(req.services()).orElse(List.of())) {
            if (!seen.add(sel.serviceId())) throw BizException.badRequest("增值服务重复");
            SolutionService sv = serviceMapper.selectById(sel.serviceId());
            if (sv == null || !sv.getSolutionId().equals(s.getId()) || sv.getStatus() != Status.ON)
                throw BizException.badRequest("所选增值服务已停售，请刷新页面");
            items.add(item(OrderItem.SERVICE, sv.getId(), sv.getName(), null, sv.getUnit(), sv.getPrice(), sel.quantity()));
        }
        BigDecimal total = items.stream().map(OrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Order o = new Order();
        o.setAccessToken(randomToken());
        o.setUserId(AuthContext.currentUser().map(AuthPrincipal::id).orElse(null));
        o.setContactName(req.contactName().trim());
        o.setContactPhone(req.contactPhone());
        o.setContactEmail(req.contactEmail().trim());
        o.setCompanyName(blankToNull(req.companyName()));
        o.setRemark(blankToNull(req.remark()));
        o.setSolutionId(s.getId());
        o.setSolutionName(s.getName());
        o.setTotalAmount(total);
        o.setStatus(OrderStatus.PENDING_PAY.name());
        o.setAgreementVersion(props.getOrder().getAgreementVersion());
        o.setPayExpireAt(now.plusMinutes(props.getOrder().getExpireMinutes()));
        insertWithUniqueNo(o, now);
        for (OrderItem i : items) {
            i.setOrderId(o.getId());
            itemMapper.insert(i);
        }
        assembler.log(o.getId(), null, OrderStatus.PENDING_PAY, "提交订单", o.getUserId() == null ? "GUEST" : "USER", o.getContactName());
        return new Created(o.getOrderNo(), o.getAccessToken(), total, o.getPayExpireAt());
    }

    /** 游客凭 token，登录用户凭账号（本人下单或手机号一致）访问订单；否则一律 404，不暴露订单是否存在。 */
    public Order requireAccessible(String orderNo, String token) {
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (o == null || !canAccess(o, token)) throw BizException.notFound("订单不存在");
        return o;
    }

    public OrderView view(String orderNo, String token) {
        return assembler.view(requireAccessible(orderNo, token));
    }

    public StatusView status(String orderNo, String token) {
        Order o = requireAccessible(orderNo, token);
        return new StatusView(o.getStatus(), o.statusEnum().label(), o.getPayExpireAt(), LocalDateTime.now().withNano(0));
    }

    /** 我的订单：本人下单的 + 用注册手机号下的游客订单 */
    public PageResult<ListItem> myOrders(long userId, String status, long page, long size) {
        AppUser u = userMapper.selectById(userId);
        Page<Order> p = orderMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<Order>()
                .and(w -> w.eq(Order::getUserId, userId).or().eq(Order::getContactPhone, u.getPhone()))
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .orderByDesc(Order::getId));
        Map<Long, String> summaries = assembler.itemSummaries(p.getRecords().stream().map(Order::getId).toList());
        return PageResult.of(p, o -> new ListItem(o.getOrderNo(), o.getStatus(), o.statusEnum().label(), o.getSolutionName(),
                o.getTotalAmount(), o.getCreatedAt(), o.getPayExpireAt(), summaries.get(o.getId())));
    }

    /** 用户取消待支付订单 */
    @Transactional
    public void cancelByUser(String orderNo, String token) {
        Order o = requireAccessible(orderNo, token);
        if (o.statusEnum() != OrderStatus.PENDING_PAY) throw BizException.conflict("只有待支付的订单可以取消");
        closeRemote(o);
        int n = orderMapper.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, o.getId()).eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                .set(Order::getStatus, OrderStatus.CANCELLED.name())
                .set(Order::getCancelledAt, LocalDateTime.now())
                .set(Order::getCancelReason, "用户取消"));
        if (n == 0) throw BizException.conflict(null);
        assembler.log(o.getId(), OrderStatus.PENDING_PAY, OrderStatus.CANCELLED, "用户取消订单", "USER", o.getContactName());
    }

    /** 已向支付平台下过单的，先关掉支付平台的订单，防止关单后仍能扫码付款 */
    void closeRemote(Order o) {
        if (o.getCodeUrl() != null) payClient.close(o.getOrderNo());
    }

    private boolean canAccess(Order o, String token) {
        if (StringUtils.hasText(token) && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), o.getAccessToken().getBytes(StandardCharsets.UTF_8))) return true;
        return AuthContext.currentUser().map(p -> {
            if (o.getUserId() != null && o.getUserId() == p.id()) return true;
            AppUser u = userMapper.selectById(p.id());
            return u != null && u.getStatus() == Status.ON && u.getPhone().equals(o.getContactPhone());
        }).orElse(false);
    }

    private void insertWithUniqueNo(Order o, LocalDateTime now) {
        for (int i = 0; ; i++) {
            o.setOrderNo(OrderNoGenerator.next(OrderNoGenerator.BIZ_SOLUTION, now));
            try {
                orderMapper.insert(o);
                return;
            } catch (DuplicateKeyException e) {
                if (i >= 4) throw e;
                log.warn("订单号冲突，重试 {}", o.getOrderNo());
            }
        }
    }

    private static OrderItem item(String type, long refId, String name, String billingType, String unit, BigDecimal price, int qty) {
        OrderItem i = new OrderItem();
        i.setItemType(type);
        i.setRefId(refId);
        i.setItemName(name);
        i.setBillingType(billingType);
        i.setUnit(unit);
        i.setUnitPrice(price);
        i.setQuantity(qty);
        i.setAmount(price.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP));
        return i;
    }

    private static String randomToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** 订单凭证：请求头优先，其次查询参数 */
    public static String pickToken(String headerToken, String queryToken) {
        return StringUtils.hasText(headerToken) ? headerToken : queryToken;
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
