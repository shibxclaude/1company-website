package com.cn01.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String accessToken;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String companyName;
    private String remark;
    private Long solutionId;
    private String solutionName;
    private BigDecimal totalAmount;
    /** 累计已退款金额：只在退款成功时用 setSql 累加，实体插入/更新时不写 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private BigDecimal refundedAmount;
    private String status;
    private String agreementVersion;
    private String payChannel;
    private String codeUrl;
    private LocalDateTime payExpireAt;
    private LocalDateTime paidAt;
    private String transactionId;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime completedAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public OrderStatus statusEnum() {
        return OrderStatus.valueOf(status);
    }
}
