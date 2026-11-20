package com.cn01.solution.dto;

import com.cn01.solution.entity.BillingType;
import com.cn01.solution.entity.DeliveryMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** 新增/编辑解决方案：基本信息 + 版本定价 + 增值服务，一次提交整体保存。 */
public record SolutionReq(
        @NotNull(message = "请选择分类") Long categoryId,
        @NotBlank(message = "URL 标识不能为空")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "URL 标识只能是小写字母、数字和短横线，如 data-fusion")
        @Size(max = 64, message = "URL 标识最多 64 字符") String slug,
        @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题最多 100 字") String name,
        @Size(max = 200, message = "副标题最多 200 字") String subtitle,
        @Size(max = 500, message = "简介最多 500 字") String summary,
        @Size(max = 255) String mainImage,
        @Size(max = 12, message = "图集最多 12 张") List<@Size(max = 255) String> gallery,
        String content,
        @NotNull(message = "请选择交付方式") DeliveryMode deliveryMode,
        @Size(max = 500, message = "售后支持最多 500 字") String afterSales,
        @Size(max = 200, message = "服务期限最多 200 字") String servicePeriod,
        @NotNull @Min(0) @Max(1) Integer recommended,
        @NotNull @Min(0) @Max(1) Integer status,
        @NotNull(message = "排序不能为空") Integer sort,
        @NotEmpty(message = "至少需要一个版本（价格）") @Size(max = 10, message = "版本最多 10 个") List<@Valid VersionReq> versions,
        @Size(max = 20, message = "增值服务最多 20 个") List<@Valid ServiceReq> services) {

    public record VersionReq(
            Long id,
            @NotBlank(message = "版本名不能为空") @Size(max = 50, message = "版本名最多 50 字") String name,
            @NotNull(message = "版本价格不能为空")
            @DecimalMin(value = "0.01", message = "价格必须大于 0")
            @DecimalMax(value = "9999999999.99", message = "价格过大")
            @Digits(integer = 10, fraction = 2, message = "价格最多两位小数") BigDecimal price,
            @DecimalMin(value = "0.01", message = "原价必须大于 0")
            @Digits(integer = 10, fraction = 2, message = "原价最多两位小数") BigDecimal originalPrice,
            @NotNull(message = "请选择计费方式") BillingType billingType,
            @Size(max = 500, message = "版本说明最多 500 字") String description,
            @NotNull @Min(0) @Max(1) Integer status,
            Integer sort) {
    }

    public record ServiceReq(
            Long id,
            @NotBlank(message = "增值服务名不能为空") @Size(max = 50, message = "增值服务名最多 50 字") String name,
            @NotNull(message = "增值服务价格不能为空")
            @DecimalMin(value = "0.01", message = "增值服务价格必须大于 0")
            @Digits(integer = 10, fraction = 2, message = "价格最多两位小数") BigDecimal price,
            @NotBlank(message = "计价单位不能为空") @Size(max = 20) String unit,
            @Size(max = 500) String description,
            @NotNull @Min(0) @Max(1) Integer status,
            Integer sort) {
    }
}
