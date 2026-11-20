package com.cn01.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_notify")
public class PaymentNotify {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channel;
    private String orderNo;
    private String transactionId;
    private String tradeState;
    private Long amountFen;
    private String result;
    private String note;
    private String raw;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
