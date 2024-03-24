package com.Fanbbs.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.Fanbbs.entity.*;
import com.Fanbbs.service.ArticleService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ArticleUitls {
    private final ArticleService articleService;
    @Autowired
    private RedisTemplate redisTemplate;
    RedisHelp redisHelp = new RedisHelp();
    private final Map<Integer, Double> hotScoreCache = new HashMap<>();

    public ArticleUitls(ArticleService articleService) {
        this.articleService = articleService;
    }

    // 定期计算和缓存所有文章的热度分数
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // 每24小时执行一次
    public void cacheAllPostHotScores() {
        List<Article> articles = articleService.selectList(new Article());
        for (Article article : articles) {
            double hotScore = calculateHotScore(article);
            hotScoreCache.put(article.getCid(), hotScore);
        }
        redisHelp.saveMapToRedis("hot_score_cache", hotScoreCache, redisTemplate, 24 * 60 * 60);
    }

    // 计算单个文章的热度分数
    public double calculateHotScore(Article article) {
        int viewCount = article.getViews() != null ? article.getViews() : 0;
        int likeCount = article.getLikes() != null ? article.getLikes() : 0;
        int commentCount = article.getCommentsNum() != null ? article.getCommentsNum() : 0;
        long createdTimestamp = article.getCreated() != null ? article.getCreated() : 0L;
        Instant createdAt = Instant.ofEpochSecond(createdTimestamp); // 将 10 位时间戳转换为 Instant
        long now = Instant.now().toEpochMilli();
        long postAgeInHours = Duration.between(createdAt, Instant.ofEpochMilli(now)).toHours(); // 帖子存活时间(小时)
        double freshness = Math.max(0.5, 1 - postAgeInHours / 24.0); // 新鲜度因子
        double hotScore = freshness * (0.2 * viewCount + 0.3 * likeCount + 0.5 * commentCount) / Math.pow(postAgeInHours + 1, 1.2);
        return hotScore;
    }

    // 新文章发布时计算热度分数并加入缓存
    public void handleNewArticle(Article article) {
        double hotScore = calculateHotScore(article);
        hotScoreCache.put(article.getCid(), hotScore);
        // 更新 Redis 中的缓存数据
        redisHelp.updateMapInRedis("hot_score_cache", hotScoreCache, redisTemplate);
    }

    // 现有文章更新时重新计算热度分数并更新缓存
    public void handleArticleUpdate(Article article) {
        double newHotScore = calculateHotScore(article);
        hotScoreCache.put(article.getCid(), newHotScore);
        // 更新 Redis 中的缓存数据
        redisHelp.updateMapInRedis("hot_score_cache", hotScoreCache, redisTemplate);
    }

    // 获取热门文章列表
// 获取热门文章列表
    public List<Article> getHotArticleList(Article query, int page, int size, String searchKey,String order,Integer random,Integer tag) {
        PageList<Article> pageList = articleService.selectPage(query, page, size, searchKey, order, random,tag);
        List<Article> articles = pageList.getList();
        List<Article> hotPosts = new ArrayList<>();

        // 从 Redis 中获取 hotScoreCache
        Map<Integer, Double> hotScoreCache = redisHelp.getMapFromRedis("hot_score_cache", redisTemplate);
        for (Article article : articles) {
            Double cachedHotScore = hotScoreCache.get(article.getCid());
            if (cachedHotScore != null) {
                article.setHotScore(cachedHotScore);
            } else {
                double hotScore = calculateHotScore(article);
                article.setHotScore(hotScore);
                hotScoreCache.put(article.getCid(), hotScore); // 更新缓存
            }
            hotPosts.add(article);
        }
        hotPosts.sort((a, b) -> Double.compare(b.getHotScore(), a.getHotScore())); // 按热度分数降序排序
        return hotPosts;
    }
}
