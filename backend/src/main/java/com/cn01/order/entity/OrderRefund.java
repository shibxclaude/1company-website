package com.cn01.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_refund")
public class OrderRefund {
    public static final String KIND_ORDER = "ORDER";
    public static final String KIND_STRAY = "STRAY";

    public enum Status {
        PROCESSING("退款中"), SUCCESS("退款成功"), FAILED("退款失败");
        private final String label;
        Status(String label) { this.label = label; }
        public String label() { return label; }
    }

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private String kind;
    private String channel;
    private String transactionId;
    private Long totalFen;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String refundId;
    private String failReason;
    private String operatorName;
    private LocalDateTime finishedAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
