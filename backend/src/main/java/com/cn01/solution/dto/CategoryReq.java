package com.cn01.solution.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryReq(
        @NotBlank(message = "分类编码不能为空")
        @Pattern(regexp = "^[a-z0-9-]{1,50}$", message = "分类编码只能是小写字母、数字、短横线")
        String code,
        @NotBlank(message = "分类名不能为空") @Size(max = 50, message = "分类名最多 50 字") String name,
        @NotNull(message = "排序不能为空") Integer sort,
        @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
}
