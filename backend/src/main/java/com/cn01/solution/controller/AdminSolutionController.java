package com.cn01.solution.controller;

import com.cn01.common.PageResult;
import com.cn01.common.Result;
import com.cn01.solution.dto.CategoryReq;
import com.cn01.solution.dto.SolutionReq;
import com.cn01.solution.dto.SolutionViews.*;
import com.cn01.solution.service.SolutionAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSolutionController {
    private final SolutionAdminService service;

    @GetMapping("/categories")
    public Result<List<CategoryView>> categories() {
        return Result.ok(service.listCategories());
    }

    @PostMapping("/categories")
    public Result<CategoryView> createCategory(@Valid @RequestBody CategoryReq req) {
        return Result.ok(service.createCategory(req));
    }

    @PutMapping("/categories/{id}")
    public Result<CategoryView> updateCategory(@PathVariable long id, @Valid @RequestBody CategoryReq req) {
        return Result.ok(service.updateCategory(id, req));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable long id) {
        service.deleteCategory(id);
        return Result.ok();
    }

    @GetMapping("/solutions")
    public Result<PageResult<AdminRow>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") @Min(1) long page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.ok(service.page(keyword, categoryId, status, page, size));
    }

    @GetMapping("/solutions/{id}")
    public Result<Detail> detail(@PathVariable long id) {
        return Result.ok(service.detail(id));
    }

    @PostMapping("/solutions")
    public Result<Detail> create(@Valid @RequestBody SolutionReq req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/solutions/{id}")
    public Result<Detail> update(@PathVariable long id, @Valid @RequestBody SolutionReq req) {
        return Result.ok(service.update(id, req));
    }

    public record StatusReq(@Min(0) @Max(1) int status) {}

    @PutMapping("/solutions/{id}/status")
    public Result<Void> status(@PathVariable long id, @Valid @RequestBody StatusReq req) {
        service.updateStatus(id, req.status());
        return Result.ok();
    }

    public record SortReq(int sort) {}

    @PutMapping("/solutions/{id}/sort")
    public Result<Void> sort(@PathVariable long id, @RequestBody SortReq req) {
        service.updateSort(id, req.sort());
        return Result.ok();
    }

    @DeleteMapping("/solutions/{id}")
    public Result<Void> delete(@PathVariable long id) {
        service.delete(id);
        return Result.ok();
    }
}
