package com.cn01.news;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news")
public class News {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String cover;
    private String content;
    private LocalDateTime publishedAt;
    private Integer top;
    private Integer status;
    private Integer views;
    @TableLogic
    private Integer deleted;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
