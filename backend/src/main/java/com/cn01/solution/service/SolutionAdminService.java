package com.cn01.solution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn01.common.BizException;
import com.cn01.common.PageResult;
import com.cn01.common.RichText;
import com.cn01.common.Status;
import com.cn01.config.CacheConfig;
import com.cn01.solution.dto.CategoryReq;
import com.cn01.solution.dto.SolutionReq;
import com.cn01.solution.dto.SolutionViews.*;
import com.cn01.solution.entity.*;
import com.cn01.solution.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolutionAdminService {
    private final SolutionCategoryMapper categoryMapper;
    private final SolutionMapper solutionMapper;
    private final SolutionVersionMapper versionMapper;
    private final SolutionServiceMapper serviceMapper;

    // ---------------- 分类 ----------------

    public List<CategoryView> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<SolutionCategory>()
                        .orderByAsc(SolutionCategory::getSort).orderByAsc(SolutionCategory::getId))
                .stream().map(SolutionAssembler::category).toList();
    }

    @CacheEvict(cacheNames = {CacheConfig.PORTAL_CATEGORIES, CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public CategoryView createCategory(CategoryReq req) {
        SolutionCategory c = new SolutionCategory();
        apply(c, req);
        categoryMapper.insert(c);
        return SolutionAssembler.category(c);
    }

    @CacheEvict(cacheNames = {CacheConfig.PORTAL_CATEGORIES, CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public CategoryView updateCategory(long id, CategoryReq req) {
        SolutionCategory c = Optional.ofNullable(categoryMapper.selectById(id)).orElseThrow(() -> BizException.notFound("分类不存在"));
        apply(c, req);
        categoryMapper.updateById(c);
        return SolutionAssembler.category(c);
    }

    @CacheEvict(cacheNames = {CacheConfig.PORTAL_CATEGORIES, CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public void deleteCategory(long id) {
        Long used = solutionMapper.selectCount(new LambdaQueryWrapper<Solution>().eq(Solution::getCategoryId, id));
        if (used > 0) throw BizException.conflict("该分类下还有 " + used + " 个解决方案，请先移走或删除");
        categoryMapper.deleteById(id);
    }

    private static void apply(SolutionCategory c, CategoryReq req) {
        c.setCode(req.code());
        c.setName(req.name().trim());
        c.setSort(req.sort());
        c.setStatus(req.status());
    }

    // ---------------- 解决方案 ----------------

    public PageResult<AdminRow> page(String keyword, Long categoryId, Integer status, long page, long size) {
        LambdaQueryWrapper<Solution> q = new LambdaQueryWrapper<Solution>()
                .eq(categoryId != null, Solution::getCategoryId, categoryId)
                .eq(status != null, Solution::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(Solution::getName, keyword).or().like(Solution::getSlug, keyword))
                .orderByAsc(Solution::getSort).orderByDesc(Solution::getId);
        Page<Solution> p = solutionMapper.selectPage(Page.of(page, Math.min(size, 100)), q);
        Set<Long> ids = p.getRecords().stream().map(Solution::getId).collect(Collectors.toSet());
        Map<Long, List<SolutionVersion>> versions = ids.isEmpty() ? Map.of() : versionMapper
                .selectList(new LambdaQueryWrapper<SolutionVersion>().in(SolutionVersion::getSolutionId, ids))
                .stream().collect(Collectors.groupingBy(SolutionVersion::getSolutionId));
        Map<Long, Long> serviceCounts = ids.isEmpty() ? Map.of() : serviceMapper
                .selectList(new LambdaQueryWrapper<SolutionService>().in(SolutionService::getSolutionId, ids))
                .stream().collect(Collectors.groupingBy(SolutionService::getSolutionId, Collectors.counting()));
        Map<Long, SolutionCategory> cats = categoryMap();
        return PageResult.of(p, s -> {
            List<SolutionVersion> vs = versions.getOrDefault(s.getId(), List.of());
            var min = vs.stream().filter(v -> v.getStatus() == Status.ON).map(SolutionVersion::getPrice).min(Comparator.naturalOrder()).orElse(null);
            SolutionCategory c = cats.get(s.getCategoryId());
            return new AdminRow(s.getId(), s.getSlug(), s.getName(), s.getMainImage(), c == null ? "—" : c.getName(),
                    min, vs.size(), serviceCounts.getOrDefault(s.getId(), 0L).intValue(),
                    s.getRecommended(), s.getStatus(), s.getSort(), s.getUpdatedAt());
        });
    }

    public Detail detail(long id) {
        Solution s = require(id);
        return SolutionAssembler.detail(s, categoryMapper.selectById(s.getCategoryId()),
                versionMapper.selectList(new LambdaQueryWrapper<SolutionVersion>().eq(SolutionVersion::getSolutionId, id)),
                serviceMapper.selectList(new LambdaQueryWrapper<SolutionService>().eq(SolutionService::getSolutionId, id)));
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public Detail create(SolutionReq req) {
        validate(req);
        Solution s = new Solution();
        apply(s, req);
        solutionMapper.insert(s);
        syncChildren(s.getId(), req);
        return detail(s.getId());
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public Detail update(long id, SolutionReq req) {
        validate(req);
        Solution s = require(id);
        apply(s, req);
        solutionMapper.updateById(s);
        syncChildren(id, req);
        return detail(id);
    }

    @CacheEvict(cacheNames = {CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public void updateStatus(long id, int status) {
        require(id);
        if (status == Status.ON) {
            Long active = versionMapper.selectCount(new LambdaQueryWrapper<SolutionVersion>()
                    .eq(SolutionVersion::getSolutionId, id).eq(SolutionVersion::getStatus, Status.ON));
            if (active == 0) throw BizException.badRequest("至少需要一个在售版本才能上架");
        }
        solutionMapper.update(new LambdaUpdateWrapper<Solution>().eq(Solution::getId, id).set(Solution::getStatus, status));
    }

    @CacheEvict(cacheNames = {CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public void updateSort(long id, int sort) {
        require(id);
        solutionMapper.update(new LambdaUpdateWrapper<Solution>().eq(Solution::getId, id).set(Solution::getSort, sort));
    }

    /** 逻辑删除；slug 改名释放，便于以后复用同一个 URL。历史订单保存了名称快照，不受影响。 */
    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.PORTAL_SOLUTIONS, CacheConfig.PORTAL_SOLUTION, CacheConfig.PORTAL_PRICE}, allEntries = true)
    public void delete(long id) {
        Solution s = require(id);
        String freed = (s.getSlug().length() > 40 ? s.getSlug().substring(0, 40) : s.getSlug()) + "-deleted-" + id;
        solutionMapper.update(new LambdaUpdateWrapper<Solution>().eq(Solution::getId, id)
                .set(Solution::getSlug, freed).set(Solution::getStatus, Status.OFF));
        solutionMapper.deleteById(id);
    }

    private void validate(SolutionReq req) {
        if (categoryMapper.selectById(req.categoryId()) == null) throw BizException.badRequest("分类不存在");
        boolean anyActive = req.versions().stream().anyMatch(v -> v.status() == Status.ON);
        if (req.status() == Status.ON && !anyActive) throw BizException.badRequest("上架状态下至少需要一个在售版本");
        for (var v : req.versions()) {
            if (v.originalPrice() != null && v.originalPrice().compareTo(v.price()) < 0)
                throw BizException.badRequest("版本「" + v.name() + "」的原价不能低于售价");
        }
    }

    private static void apply(Solution s, SolutionReq req) {
        s.setCategoryId(req.categoryId());
        s.setSlug(req.slug());
        s.setName(req.name().trim());
        s.setSubtitle(blankToNull(req.subtitle()));
        s.setSummary(blankToNull(req.summary()));
        s.setMainImage(blankToNull(req.mainImage()));
        s.setGallery(req.gallery() == null ? new ArrayList<>() : req.gallery().stream().filter(StringUtils::hasText).toList());
        s.setContent(RichText.clean(req.content()));
        s.setDeliveryMode(req.deliveryMode().name());
        s.setAfterSales(blankToNull(req.afterSales()));
        s.setServicePeriod(blankToNull(req.servicePeriod()));
        s.setRecommended(req.recommended());
        s.setStatus(req.status());
        s.setSort(req.sort());
    }

    /** 版本与增值服务：带 id 的更新，不带 id 的新增，请求中没有的删除。 */
    private void syncChildren(long solutionId, SolutionReq req) {
        Map<Long, SolutionVersion> oldVersions = versionMapper.selectList(new LambdaQueryWrapper<SolutionVersion>()
                .eq(SolutionVersion::getSolutionId, solutionId)).stream().collect(Collectors.toMap(SolutionVersion::getId, Function.identity()));
        Set<Long> keep = new HashSet<>();
        int i = 0;
        for (var v : req.versions()) {
            i++;
            SolutionVersion e = v.id() != null ? oldVersions.get(v.id()) : null;
            if (v.id() != null && e == null) throw BizException.badRequest("版本 id 不属于该解决方案：" + v.id());
            if (e == null) e = new SolutionVersion();
            e.setSolutionId(solutionId);
            e.setName(v.name().trim());
            e.setPrice(v.price());
            e.setOriginalPrice(v.originalPrice());
            e.setBillingType(v.billingType().name());
            e.setDescription(blankToNull(v.description()));
            e.setStatus(v.status());
            e.setSort(v.sort() == null ? i : v.sort());
            if (e.getId() == null) versionMapper.insert(e);
            else { versionMapper.updateById(e); keep.add(e.getId()); }
        }
        oldVersions.keySet().stream().filter(id -> !keep.contains(id)).forEach(versionMapper::deleteById);

        Map<Long, SolutionService> oldServices = serviceMapper.selectList(new LambdaQueryWrapper<SolutionService>()
                .eq(SolutionService::getSolutionId, solutionId)).stream().collect(Collectors.toMap(SolutionService::getId, Function.identity()));
        Set<Long> keepS = new HashSet<>();
        i = 0;
        for (var v : Optional.ofNullable(req.services()).orElse(List.of())) {
            i++;
            SolutionService e = v.id() != null ? oldServices.get(v.id()) : null;
            if (v.id() != null && e == null) throw BizException.badRequest("增值服务 id 不属于该解决方案：" + v.id());
            if (e == null) e = new SolutionService();
            e.setSolutionId(solutionId);
            e.setName(v.name().trim());
            e.setPrice(v.price());
            e.setUnit(v.unit().trim());
            e.setDescription(blankToNull(v.description()));
            e.setStatus(v.status());
            e.setSort(v.sort() == null ? i : v.sort());
            if (e.getId() == null) serviceMapper.insert(e);
            else { serviceMapper.updateById(e); keepS.add(e.getId()); }
        }
        oldServices.keySet().stream().filter(id -> !keepS.contains(id)).forEach(serviceMapper::deleteById);
    }

    private Solution require(long id) {
        return Optional.ofNullable(solutionMapper.selectById(id)).orElseThrow(() -> BizException.notFound("解决方案不存在"));
    }

    private Map<Long, SolutionCategory> categoryMap() {
        return categoryMapper.selectList(null).stream().collect(Collectors.toMap(SolutionCategory::getId, Function.identity()));
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
