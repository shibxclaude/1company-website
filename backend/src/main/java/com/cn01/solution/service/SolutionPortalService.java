package com.cn01.solution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn01.common.BizException;
import com.cn01.common.Status;
import com.cn01.config.CacheConfig;
import com.cn01.solution.dto.SolutionViews.*;
import com.cn01.solution.entity.*;
import com.cn01.solution.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 前台只读查询，结果缓存在 Redis；后台写操作会清空缓存。
 * 只返回"上架 + 分类启用"的方案，以及其中"在售"的版本和增值服务。
 */
@Service
@RequiredArgsConstructor
public class SolutionPortalService {
    private final SolutionCategoryMapper categoryMapper;
    private final SolutionMapper solutionMapper;
    private final SolutionVersionMapper versionMapper;
    private final SolutionServiceMapper serviceMapper;

    @Cacheable(cacheNames = CacheConfig.PORTAL_CATEGORIES, key = "'all'")
    public ArrayList<CategoryView> categories() {
        return activeCategories().values().stream()
                .sorted(Comparator.comparing(SolutionCategory::getSort).thenComparing(SolutionCategory::getId))
                .map(SolutionAssembler::category).collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = CacheConfig.PORTAL_SOLUTIONS, key = "'all'")
    public ArrayList<Card> listOnSale() {
        Map<Long, SolutionCategory> cats = activeCategories();
        List<Solution> solutions = onSaleSolutions(cats);
        Map<Long, List<SolutionVersion>> versions = activeVersions(solutions);
        return solutions.stream()
                .map(s -> SolutionAssembler.card(s, cats.get(s.getCategoryId()), versions.getOrDefault(s.getId(), List.of())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(cacheNames = CacheConfig.PORTAL_SOLUTION, key = "#slug")
    public Detail detail(String slug) {
        Solution s = solutionMapper.selectOne(new LambdaQueryWrapper<Solution>().eq(Solution::getSlug, slug).eq(Solution::getStatus, Status.ON));
        SolutionCategory c = s == null ? null : categoryMapper.selectById(s.getCategoryId());
        if (s == null || c == null || c.getStatus() != Status.ON) throw BizException.notFound("解决方案不存在或已下架");
        return SolutionAssembler.detail(s, c,
                versionMapper.selectList(new LambdaQueryWrapper<SolutionVersion>().eq(SolutionVersion::getSolutionId, s.getId()).eq(SolutionVersion::getStatus, Status.ON)),
                serviceMapper.selectList(new LambdaQueryWrapper<SolutionService>().eq(SolutionService::getSolutionId, s.getId()).eq(SolutionService::getStatus, Status.ON)));
    }

    @Cacheable(cacheNames = CacheConfig.PORTAL_PRICE, key = "'all'")
    public PricePage pricePage() {
        Map<Long, SolutionCategory> cats = activeCategories();
        List<Solution> solutions = onSaleSolutions(cats);
        Map<Long, List<SolutionVersion>> versions = activeVersions(solutions);
        Map<Long, List<SolutionService>> services = solutions.isEmpty() ? Map.of() : serviceMapper
                .selectList(new LambdaQueryWrapper<SolutionService>()
                        .in(SolutionService::getSolutionId, solutions.stream().map(Solution::getId).toList())
                        .eq(SolutionService::getStatus, Status.ON))
                .stream().collect(Collectors.groupingBy(SolutionService::getSolutionId));
        ArrayList<PriceSolution> rows = solutions.stream().map(s -> new PriceSolution(s.getId(), s.getSlug(), s.getName(),
                        cats.get(s.getCategoryId()).getName(), DeliveryMode.valueOf(s.getDeliveryMode()).label(), s.getServicePeriod(),
                        SolutionAssembler.versions(versions.getOrDefault(s.getId(), List.of())),
                        SolutionAssembler.services(services.getOrDefault(s.getId(), List.of()))))
                .collect(Collectors.toCollection(ArrayList::new));
        return new PricePage(rows, LocalDateTime.now().withNano(0));
    }

    private Map<Long, SolutionCategory> activeCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<SolutionCategory>().eq(SolutionCategory::getStatus, Status.ON))
                .stream().collect(Collectors.toMap(SolutionCategory::getId, Function.identity()));
    }

    private List<Solution> onSaleSolutions(Map<Long, SolutionCategory> cats) {
        if (cats.isEmpty()) return List.of();
        return solutionMapper.selectList(new LambdaQueryWrapper<Solution>()
                .eq(Solution::getStatus, Status.ON).in(Solution::getCategoryId, cats.keySet())
                .orderByAsc(Solution::getSort).orderByAsc(Solution::getId));
    }

    private Map<Long, List<SolutionVersion>> activeVersions(List<Solution> solutions) {
        if (solutions.isEmpty()) return Map.of();
        return versionMapper.selectList(new LambdaQueryWrapper<SolutionVersion>()
                        .in(SolutionVersion::getSolutionId, solutions.stream().map(Solution::getId).toList())
                        .eq(SolutionVersion::getStatus, Status.ON))
                .stream().collect(Collectors.groupingBy(SolutionVersion::getSolutionId));
    }
}
