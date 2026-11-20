package com.cn01.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String username;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private LocalDateTime lastLoginAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
