package com.Fanbbs.event;

import org.springframework.context.ApplicationEvent;
import com.Fanbbs.entity.Article;
public class NewArticleEvent extends ApplicationEvent  {
    public NewArticleEvent(Article source) {
        super(source);
    }

    public Article getArticle() {
        return (Article) getSource();
    }
}
