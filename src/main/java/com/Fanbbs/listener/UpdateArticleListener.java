package com.Fanbbs.listener;

import com.Fanbbs.common.ArticleUitls;
import com.Fanbbs.event.UpdateArticleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import com.Fanbbs.entity.Article;
import org.springframework.stereotype.Component;

@Component
public class UpdateArticleListener implements ApplicationListener<UpdateArticleEvent> {
    @Autowired
    private ArticleUitls articleUitls;
    @Override
    public void onApplicationEvent(UpdateArticleEvent updateArticleEvent) {
        Article updateArticle = updateArticleEvent.getArticle();
        articleUitls.handleArticleUpdate(updateArticle);
    }
}
