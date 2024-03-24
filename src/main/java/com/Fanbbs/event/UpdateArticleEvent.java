package com.Fanbbs.event;

import org.springframework.context.ApplicationEvent;
import com.Fanbbs.entity.Article;
public class UpdateArticleEvent  extends ApplicationEvent {
    public UpdateArticleEvent(Article source) {
        super(source);
    }

    public Article getArticle() {
        return (Article) getSource();
    }
}
