package com.cn01.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateReq(
            @NotBlank(message = "缺少解决方案") String slug,
            @NotNull(message = "请选择版本") Long versionId,
            @NotNull(message = "请填写购买数量") @Min(value = 1, message = "购买数量至少为 1") @Max(value = 99, message = "购买数量最多 99") Integer quantity,
            @Size(max = 20) List<@Valid ServiceSel> services,
            @NotBlank(message = "请填写姓名") @Size(max = 50, message = "姓名最多 50 字") String contactName,
            @NotBlank(message = "请填写手机号") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String contactPhone,
            @NotBlank(message = "请填写邮箱") @Email(message = "邮箱格式不正确") @Size(max = 100) String contactEmail,
            @Size(max = 100, message = "企业名称最多 100 字") String companyName,
            @Size(max = 500, message = "备注最多 500 字") String remark,
            @AssertTrue(message = "请阅读并同意《产品购买服务协议》") boolean agreed,
            /** 下单页上显示给用户的应付金额；与服务端按当前价格算出的不一致时拒绝下单（不带则不校验） */
            @Digits(integer = 10, fraction = 2, message = "金额格式不正确") BigDecimal expectedTotal) {}

    public record ServiceSel(@NotNull Long serviceId,
                             @NotNull @Min(value = 1, message = "增值服务数量至少为 1") @Max(value = 99, message = "增值服务数量最多 99") Integer quantity) {}

    public record Created(String orderNo, String accessToken, BigDecimal totalAmount, LocalDateTime payExpireAt) {}

    public record ItemView(String itemType, String itemName, String billingType, String unit,
                           BigDecimal unitPrice, int quantity, BigDecimal amount) {}

    public record LogView(String fromStatus, String toStatus, String toStatusLabel, String note,
                          String operatorType, LocalDateTime createdAt) {}

    /** 订单详情（前台用户/游客、后台共用；后台多出 userId 等字段由 AdminView 承载） */
    public record OrderView(String orderNo, String status, String statusLabel, String solutionName, String solutionSlug,
                            BigDecimal totalAmount, BigDecimal refundedAmount, String contactName, String contactPhone, String contactEmail,
                            String companyName, String remark, String agreementVersion, String payChannel,
                            LocalDateTime payExpireAt, LocalDateTime paidAt, LocalDateTime cancelledAt, String cancelReason,
                            LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime serverTime,
                            List<ItemView> items, List<LogView> logs) {}

    public record ListItem(String orderNo, String status, String statusLabel, String solutionName,
                           BigDecimal totalAmount, LocalDateTime createdAt, LocalDateTime payExpireAt, String itemSummary) {}

    public record AdminListItem(String orderNo, String status, String statusLabel, String solutionName,
                                BigDecimal totalAmount, String contactName, String contactPhone, String companyName,
                                Long userId, String payChannel, LocalDateTime createdAt, LocalDateTime paidAt) {}

    public record AdminDetail(OrderView order, Long userId, String transactionId, List<String> nextStatuses,
                              BigDecimal refundableAmount, List<RefundView> refunds, List<AbnormalPayment> abnormalPayments) {}

    public record RefundView(String refundNo, String kind, String transactionId, BigDecimal amount, String status, String statusLabel,
                             String reason, String failReason, String operatorName, LocalDateTime createdAt, LocalDateTime finishedAt) {}

    /** 异常收款：支付平台已扣款但没有入账到订单的交易（重复付款、金额不符），可整笔退回 */
    public record AbnormalPayment(String transactionId, BigDecimal amount, String note, LocalDateTime createdAt, boolean refunded) {}

    public record RefundReq(@NotNull(message = "请填写退款金额") @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
                            @Digits(integer = 10, fraction = 2, message = "退款金额最多两位小数") BigDecimal amount,
                            @NotBlank(message = "请填写退款原因") @Size(max = 200, message = "退款原因最多 200 字") String reason) {}

    public record StrayRefundReq(@NotBlank(message = "缺少交易号") String transactionId,
                                 @NotBlank(message = "请填写退款原因") @Size(max = 200, message = "退款原因最多 200 字") String reason) {}

    public record ProgressReq(String toStatus, @Size(max = 500, message = "备注最多 500 字") String note) {}

    public record StatusView(String status, String statusLabel, LocalDateTime payExpireAt, LocalDateTime serverTime) {}

    public record PayInfo(String codeUrl, String channel, boolean mock, BigDecimal totalAmount,
                          LocalDateTime payExpireAt, LocalDateTime serverTime) {}
}
