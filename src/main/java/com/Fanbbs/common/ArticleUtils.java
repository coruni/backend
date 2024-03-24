package com.Fanbbs.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.Fanbbs.entity.*;
import com.Fanbbs.service.ArticleService;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class ArticleUtils {
    private final ArticleService articleService;
    @Autowired
    private RedisTemplate redisTemplate;
    RedisHelp redisHelp = new RedisHelp();
    private final Map<Integer, Double> hotScoreCache = new HashMap<>();
    private List<Article> allArticles = new ArrayList<>();

    public ArticleUtils(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostConstruct
    public void init() {
        Map<Integer, Double> hotScoreCache = redisHelp.getMapFromRedis("hot_score_cache", redisTemplate);
        if (hotScoreCache == null || hotScoreCache.isEmpty()) {
            rebuildCache();
        } else {
            List<Article> articles = articleService.selectList(new Article());
            allArticles.addAll(articles);
            // Collections.shuffle(allArticles);
        }
    }

    @Scheduled(fixedRate = 24 * 60 * 60)
    public void rebuildCache() {
        List<Article> articles = articleService.selectList(new Article());
        allArticles.clear();
        allArticles.addAll(articles);
        //Collections.shuffle(allArticles);

        Map<Integer, Double> hotScoreCache = new HashMap<>();
        for (Article article : allArticles) {
            double hotScore = calculateHotScore(article);
            hotScoreCache.put(article.getCid(), hotScore);
        }
        redisHelp.saveMapToRedis("hot_score_cache", hotScoreCache, redisTemplate, 24 * 60 * 60);
    }

    @Async
    public CompletableFuture<Void> updateArticleListAsync(Article article) {
        return CompletableFuture.runAsync(() -> {
            int index = allArticles.indexOf(article);
            if (index >= 0) {
                allArticles.set(index, article);
            } else {
                allArticles.add(article);
            }
//            Collections.shuffle(allArticles);
        });
    }
    // 定期计算和缓存所有文章的热度分数
//    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // 每24小时执行一次
//    public void cacheAllPostHotScores() {
//        List<Article> articles = articleService.selectList(new Article());
//        allArticles.clear();
//        allArticles.addAll(articles);
//        Collections.shuffle(allArticles);
//        for (Article article : articles) {
//            double hotScore = calculateHotScore(article);
//            hotScoreCache.put(article.getCid(), hotScore);
//        }
//        redisHelp.saveMapToRedis("hot_score_cache", hotScoreCache, redisTemplate, 24 * 60 * 60);
//    }

    // 计算单个文章的热度分数
    public double calculateHotScore(Article article) {
        int viewCount = article.getViews() != null ? article.getViews() : 0;
        int likeCount = article.getLikes() != null ? article.getLikes() : 0;
        int commentCount = article.getCommentsNum() != null ? article.getCommentsNum() : 0;
        long createdTimestamp = article.getCreated() != null ? article.getCreated() : 0L;
        Instant createdAt = Instant.ofEpochSecond(createdTimestamp); // 将 10 位时间戳转换为 Instant
        long now = Instant.now().toEpochMilli();
        long postAgeInHours = Duration.between(createdAt, Instant.ofEpochMilli(now)).toHours(); // 帖子存活时间(小时)
        double freshness = Math.max(0.8, 1 - postAgeInHours / 24.0); // 新鲜度因子, 最小值为 0.8
        double hotScore = freshness * (0.2 * viewCount + 0.3 * likeCount + 0.5 * commentCount + 1.0 / Math.pow(postAgeInHours + 1, 1.5));
        return hotScore;
    }

    // 新文章发布时计算热度分数并加入缓存
    public void handleNewArticle(Article article) {
        double hotScore = calculateHotScore(article);
        hotScoreCache.put(article.getCid(), hotScore);
        allArticles.add(article);
//        Collections.shuffle(allArticles);
        updateArticleListAsync(article);
        redisHelp.updateMapInRedis("hot_score_cache", hotScoreCache, redisTemplate);
    }

    // 现有文章更新时重新计算热度分数并更新缓存
    public void handleArticleUpdate(Article article) {
        double newHotScore = calculateHotScore(article);
        hotScoreCache.put(article.getCid(), newHotScore);
        int index = allArticles.indexOf(article);
        if (index >= 0) {
            allArticles.set(index, article);
        }
//        Collections.shuffle(allArticles);
        updateArticleListAsync(article);
        redisHelp.updateMapInRedis("hot_score_cache", hotScoreCache, redisTemplate);
    }

    // 获取热门文章列表
    public List<Article> getHotArticleList(int page, int size, Integer mid) {
        List<Article> hotPosts = new ArrayList<>();
        Map<Integer, Double> hotScoreCache = redisHelp.getMapFromRedis("hot_score_cache", redisTemplate);

        if (hotScoreCache == null || hotScoreCache.isEmpty() || allArticles.isEmpty()) {
            rebuildCache();
            hotScoreCache = redisHelp.getMapFromRedis("hot_score_cache", redisTemplate);
        }

        // 在获取热门文章列表之前对 allArticles 进行随机打乱
        // Collections.shuffle(allArticles);

        int startIndex = (page - 1) * size;
        int endIndex = startIndex + size;

        for (int i = startIndex; i < endIndex && i < allArticles.size(); i++) {
            Article article = allArticles.get(i);

            // 判断 mid 是否匹配
            if (mid != null && !mid.equals(article.getMid())) {
                continue; // 如果不匹配，跳过该文章
            }

            Double cachedHotScore = hotScoreCache.get(article.getCid());
            if (cachedHotScore != null) {
                article.setHotScore(cachedHotScore);
            } else {
                double hotScore = calculateHotScore(article);
                article.setHotScore(hotScore);
                hotScoreCache.put(article.getCid(), hotScore);
            }
            hotPosts.add(article);
        }
        hotPosts.sort(Comparator.comparingDouble(Article::getHotScore).reversed());
        return hotPosts;
    }
}