package com.Fanbbs.listener;

import com.Fanbbs.common.ArticleUitls;
import com.Fanbbs.event.NewArticleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.Fanbbs.entity.Article;

@Component
public class NewArticleListener implements ApplicationListener<NewArticleEvent> {

    @Autowired
    private ArticleUitls articleUitls;

    @Override
    public void onApplicationEvent(NewArticleEvent newArticleEvent) {
        Article newArticle = newArticleEvent.getArticle();
        articleUitls.handleNewArticle(newArticle);
    }
}
