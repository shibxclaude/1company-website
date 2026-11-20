package com.cn01.news;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn01.common.Status;
import com.cn01.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时发布：新闻列表缓存 30 分钟，到点的新闻要等缓存过期才出现。
 * 每分钟检查一次最近 90 秒内到点的新闻，有就清掉前台新闻缓存（多实例各自执行也无妨，清缓存是幂等的）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsPublishJob {
    private final NewsMapper mapper;
    private final CacheManager cacheManager;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        evictIfDue(LocalDateTime.now());
    }

    /** @return 是否清了缓存 */
    public boolean evictIfDue(LocalDateTime now) {
        long due = mapper.selectCount(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, Status.ON)
                .gt(News::getPublishedAt, now.minusSeconds(90))
                .le(News::getPublishedAt, now));
        if (due == 0) return false;
        Cache cache = cacheManager.getCache(CacheConfig.PORTAL_NEWS);
        if (cache != null) cache.clear();
        log.info("定时新闻到点 {} 条，已刷新前台新闻缓存", due);
        return true;
    }
}
