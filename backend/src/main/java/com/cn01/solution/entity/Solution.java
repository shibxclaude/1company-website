package com.cn01.solution.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "solution", autoResultMap = true)
public class Solution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String slug;
    private String name;
    private String subtitle;
    private String summary;
    private String mainImage;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> gallery;
    private String content;
    private String deliveryMode;
    private String afterSales;
    private String servicePeriod;
    private Integer recommended;
    private Integer status;
    private Integer sort;
    @TableLogic
    private Integer deleted;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
