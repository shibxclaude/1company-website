package com.cn01.solution.controller;

import com.cn01.common.Result;
import com.cn01.solution.dto.SolutionViews.*;
import com.cn01.solution.service.SolutionPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 前台公开接口，无需登录（价格对访客公开可见）。 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalSolutionController {
    private final SolutionPortalService service;

    @GetMapping("/categories")
    public Result<List<CategoryView>> categories() {
        return Result.ok(service.categories());
    }

    /** 分类筛选与关键字搜索在缓存后的全量列表上做（方案数量很少） */
    @GetMapping("/solutions")
    public Result<List<Card>> list(@RequestParam(required = false) String category,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Boolean recommended) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        List<Card> list = service.listOnSale().stream()
                .filter(c -> !StringUtils.hasText(category) || category.equals(c.categoryCode()))
                .filter(c -> kw == null || contains(c.name(), kw) || contains(c.subtitle(), kw) || contains(c.summary(), kw))
                .filter(c -> recommended == null || !recommended || c.recommended() == 1)
                .toList();
        return Result.ok(list);
    }

    @GetMapping("/solutions/{slug}")
    public Result<Detail> detail(@PathVariable String slug) {
        return Result.ok(service.detail(slug));
    }

    @GetMapping("/price")
    public Result<PricePage> price() {
        return Result.ok(service.pricePage());
    }

    private static boolean contains(String s, String kw) {
        return s != null && s.toLowerCase().contains(kw);
    }
}
