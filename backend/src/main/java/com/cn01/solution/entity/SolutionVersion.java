package com.cn01.solution.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("solution_version")
public class SolutionVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long solutionId;
    private String name;
    private BigDecimal price;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal originalPrice;
    private String billingType;
    private String description;
    private Integer status;
    private Integer sort;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
