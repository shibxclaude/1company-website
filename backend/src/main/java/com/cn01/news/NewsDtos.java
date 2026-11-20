package com.cn01.news;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;

public final class NewsDtos {
    private NewsDtos() {}

    public record NewsReq(
            @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题最多 200 字") String title,
            @Size(max = 500, message = "摘要最多 500 字") String summary,
            @Size(max = 255) String cover,
            @NotBlank(message = "正文不能为空") String content,
            LocalDateTime publishedAt,
            @NotNull @Min(0) @Max(1) Integer top,
            @NotNull @Min(0) @Max(1) Integer status) {}

    public record NewsItem(Long id, String title, String summary, String cover, LocalDateTime publishedAt,
                           Integer top, Integer status, Integer views) implements Serializable {}

    public record NewsDetail(Long id, String title, String summary, String cover, String content,
                             LocalDateTime publishedAt, Integer top, Integer status, Integer views) implements Serializable {}

    public record NewsPage(ArrayList<NewsItem> list, long total, long page, long size) implements Serializable {}

    static NewsItem item(News n) {
        return new NewsItem(n.getId(), n.getTitle(), n.getSummary(), n.getCover(), n.getPublishedAt(), n.getTop(), n.getStatus(), n.getViews());
    }

    static NewsDetail detail(News n) {
        return new NewsDetail(n.getId(), n.getTitle(), n.getSummary(), n.getCover(), n.getContent(), n.getPublishedAt(),
                n.getTop(), n.getStatus(), n.getViews());
    }
}
