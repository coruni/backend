package com.Fanbbs.listener;

import com.Fanbbs.common.ArticleUtils;
import com.Fanbbs.event.NewArticleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.Fanbbs.entity.Article;

@Component
public class NewArticleListener implements ApplicationListener<NewArticleEvent> {

    @Autowired
    private ArticleUtils articleUtils;

    @Override
    public void onApplicationEvent(NewArticleEvent newArticleEvent) {
        Article newArticle = newArticleEvent.getArticle();
        articleUtils.handleNewArticle(newArticle);
    }
}
