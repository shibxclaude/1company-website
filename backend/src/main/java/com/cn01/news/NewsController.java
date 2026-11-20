package com.cn01.news;

import com.cn01.common.PageResult;
import com.cn01.common.Result;
import com.cn01.news.NewsDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
public class NewsController {
    private final NewsService service;

    @GetMapping("/api/portal/news")
    public Result<NewsPage> portalPage(@RequestParam(defaultValue = "1") @Min(1) long page,
                                       @RequestParam(defaultValue = "10") @Min(1) @Max(50) long size) {
        return Result.ok(service.portalPage(page, size));
    }

    @GetMapping("/api/portal/news/{id}")
    public Result<NewsDetail> portalDetail(@PathVariable long id) {
        return Result.ok(service.portalDetail(id));
    }

    @GetMapping("/api/admin/news")
    public Result<PageResult<NewsItem>> adminPage(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(defaultValue = "1") @Min(1) long page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.ok(service.adminPage(keyword, status, page, size));
    }

    @GetMapping("/api/admin/news/{id}")
    public Result<NewsDetail> adminDetail(@PathVariable long id) {
        return Result.ok(service.adminDetail(id));
    }

    @PostMapping("/api/admin/news")
    public Result<NewsDetail> create(@Valid @RequestBody NewsReq req) {
        return Result.ok(service.create(req));
    }

    @PutMapping("/api/admin/news/{id}")
    public Result<NewsDetail> update(@PathVariable long id, @Valid @RequestBody NewsReq req) {
        return Result.ok(service.update(id, req));
    }

    public record StatusReq(@Min(0) @Max(1) int status) {}

    @PutMapping("/api/admin/news/{id}/status")
    public Result<Void> status(@PathVariable long id, @Valid @RequestBody StatusReq req) {
        service.updateStatus(id, req.status());
        return Result.ok();
    }

    @DeleteMapping("/api/admin/news/{id}")
    public Result<Void> delete(@PathVariable long id) {
        service.delete(id);
        return Result.ok();
    }
}
