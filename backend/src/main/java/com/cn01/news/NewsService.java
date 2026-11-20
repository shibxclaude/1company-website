package com.cn01.news;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn01.common.BizException;
import com.cn01.common.PageResult;
import com.cn01.common.RichText;
import com.cn01.common.Status;
import com.cn01.config.CacheConfig;
import com.cn01.news.NewsDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsMapper mapper;
    private final StringRedisTemplate redis;

    // ---------- 前台 ----------

    @Cacheable(cacheNames = CacheConfig.PORTAL_NEWS, key = "'page:' + #page + ':' + #size")
    public NewsPage portalPage(long page, long size) {
        Page<News> p = mapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<News>()
                .select(News::getId, News::getTitle, News::getSummary, News::getCover, News::getPublishedAt,
                        News::getTop, News::getStatus, News::getViews)
                .eq(News::getStatus, Status.ON)
                .le(News::getPublishedAt, LocalDateTime.now())
                .orderByDesc(News::getTop).orderByDesc(News::getPublishedAt).orderByDesc(News::getId));
        return new NewsPage(p.getRecords().stream().map(NewsDtos::item).collect(Collectors.toCollection(ArrayList::new)),
                p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Cacheable(cacheNames = CacheConfig.PORTAL_NEWS, key = "'detail:' + #id")
    public NewsDetail portalDetail(long id) {
        News n = mapper.selectById(id);
        if (n == null || n.getStatus() != Status.ON || n.getPublishedAt().isAfter(LocalDateTime.now()))
            throw BizException.notFound("新闻不存在");
        return NewsDtos.detail(n);
    }

    /** 阅读数 +1：同一 IP 一小时内同一篇只计一次。不清缓存，前台显示的阅读数允许滞后。 */
    public void recordView(long id, String ip) {
        Boolean first = redis.opsForValue().setIfAbsent("cn01:news-view:" + id + ":" + ip, "1", Duration.ofHours(1));
        if (!Boolean.TRUE.equals(first)) return;
        // updated_at = updated_at：阅读数变化不算"内容更新"
        mapper.update(null, new LambdaUpdateWrapper<News>().eq(News::getId, id).eq(News::getStatus, Status.ON)
                .setSql("views = views + 1, updated_at = updated_at"));
    }

    // ---------- 后台 ----------

    public PageResult<NewsItem> adminPage(String keyword, Integer status, long page, long size) {
        Page<News> p = mapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<News>()
                .select(News::getId, News::getTitle, News::getSummary, News::getCover, News::getPublishedAt,
                        News::getTop, News::getStatus, News::getViews)
                .like(StringUtils.hasText(keyword), News::getTitle, keyword)
                .eq(status != null, News::getStatus, status)
                .orderByDesc(News::getTop).orderByDesc(News::getPublishedAt).orderByDesc(News::getId));
        return PageResult.of(p, NewsDtos::item);
    }

    public NewsDetail adminDetail(long id) {
        return NewsDtos.detail(require(id));
    }

    @CacheEvict(cacheNames = CacheConfig.PORTAL_NEWS, allEntries = true)
    public NewsDetail create(NewsReq req) {
        News n = new News();
        n.setViews(0);
        apply(n, req);
        mapper.insert(n);
        return NewsDtos.detail(n);
    }

    @CacheEvict(cacheNames = CacheConfig.PORTAL_NEWS, allEntries = true)
    public NewsDetail update(long id, NewsReq req) {
        News n = require(id);
        apply(n, req);
        mapper.updateById(n);
        return NewsDtos.detail(n);
    }

    @CacheEvict(cacheNames = CacheConfig.PORTAL_NEWS, allEntries = true)
    public void updateStatus(long id, int status) {
        require(id);
        mapper.update(new LambdaUpdateWrapper<News>().eq(News::getId, id).set(News::getStatus, status));
    }

    @CacheEvict(cacheNames = CacheConfig.PORTAL_NEWS, allEntries = true)
    public void delete(long id) {
        require(id);
        mapper.deleteById(id);
    }

    private News require(long id) {
        return Optional.ofNullable(mapper.selectById(id)).orElseThrow(() -> BizException.notFound("新闻不存在"));
    }

    private static void apply(News n, NewsReq req) {
        n.setTitle(req.title().trim());
        n.setSummary(StringUtils.hasText(req.summary()) ? req.summary().trim() : null);
        n.setCover(StringUtils.hasText(req.cover()) ? req.cover() : null);
        n.setContent(RichText.clean(req.content()));
        n.setPublishedAt(req.publishedAt() != null ? req.publishedAt() : LocalDateTime.now().withNano(0));
        n.setTop(req.top());
        n.setStatus(req.status());
    }
}
