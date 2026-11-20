package com.cn01.solution.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 增值服务（运维期、部署、培训等） */
@Data
@TableName("solution_service")
public class SolutionService {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long solutionId;
    private String name;
    private BigDecimal price;
    private String unit;
    private String description;
    private Integer status;
    private Integer sort;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
