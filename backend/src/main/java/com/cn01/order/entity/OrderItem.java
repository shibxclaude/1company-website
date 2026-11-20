package com.cn01.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("order_item")
public class OrderItem {
    public static final String VERSION = "VERSION";
    public static final String SERVICE = "SERVICE";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String itemType;
    private Long refId;
    private String itemName;
    private String billingType;
    private String unit;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal amount;
}
