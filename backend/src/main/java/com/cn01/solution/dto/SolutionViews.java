package com.cn01.solution.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 接口返回对象。注意：会被缓存到 Redis 的对象里，集合一律用 ArrayList（不可变集合无法反序列化）。
 */
public final class SolutionViews {
    private SolutionViews() {}

    public record CategoryView(Long id, String code, String name, Integer sort, Integer status) implements Serializable {}

    public record VersionView(Long id, String name, BigDecimal price, BigDecimal originalPrice, String billingType,
                              String billingLabel, String unit, String description, Integer status, Integer sort) implements Serializable {}

    public record ServiceView(Long id, String name, BigDecimal price, String unit, String description,
                              Integer status, Integer sort) implements Serializable {}

    /** 前台列表卡片 */
    public record Card(Long id, String slug, String name, String subtitle, String summary, String mainImage,
                       Long categoryId, String categoryCode, String categoryName,
                       BigDecimal minPrice, BigDecimal minOriginalPrice, String priceUnit, String priceBillingLabel,
                       boolean multipleVersions, Integer recommended, Integer sort) implements Serializable {}

    /** 前台详情 / 后台编辑 */
    public record Detail(Long id, String slug, String name, String subtitle, String summary, String mainImage,
                         ArrayList<String> gallery, String content, Long categoryId, String categoryCode, String categoryName,
                         String deliveryMode, String deliveryLabel, String afterSales, String servicePeriod,
                         Integer recommended, Integer status, Integer sort,
                         ArrayList<VersionView> versions, ArrayList<ServiceView> services,
                         LocalDateTime updatedAt) implements Serializable {}

    /** 后台列表行 */
    public record AdminRow(Long id, String slug, String name, String mainImage, String categoryName,
                           BigDecimal minPrice, int versionCount, int serviceCount,
                           Integer recommended, Integer status, Integer sort, LocalDateTime updatedAt) {}

    /** 价格公示页 */
    public record PriceSolution(Long id, String slug, String name, String categoryName, String deliveryLabel,
                                String servicePeriod, ArrayList<VersionView> versions,
                                ArrayList<ServiceView> services) implements Serializable {}

    public record PricePage(ArrayList<PriceSolution> solutions, LocalDateTime generatedAt) implements Serializable {}
}
