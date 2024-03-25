package com.Fanbbs.service.impl;

import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;
import com.Fanbbs.dao.*;
import com.Fanbbs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务层实现类
 * TypechoContentsServiceImpl
 *
 * @author buxia97
 * @date 2021/11/29
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    ArticleDao dao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public int insert(Article article) {
        return dao.insert(article);
    }

    @Override
    public int batchInsert(List<Article> list) {
        return dao.batchInsert(list);
    }

    @Override
    public int update(Article article) {
        return dao.update(article);
    }

    @Override
    public int delete(Object key) {
        return dao.delete(key);
    }

    @Override
    public int batchDelete(List<Object> keys) {
        return dao.batchDelete(keys);
    }

    @Override
    public Article selectByKey(Object key) {
        return dao.selectByKey(key);
    }

    @Override
    public List<Article> selectList(Article article) {
        return dao.selectList(article);
    }

    @Override
    public PageList<Article> selectPage(Article article, Integer offset, Integer pageSize, String searchKey, String order, Integer random, Integer tag) {
        PageList<Article> pageList = new PageList<>();

        int total = this.total(article, searchKey);

        int totalPage;
        if (total % pageSize != 0) {
            totalPage = (total / pageSize) + 1;
        } else {
            totalPage = total / pageSize;
        }

        int page = (offset - 1) * pageSize;

        List<Article> list = dao.selectPage(article, page, pageSize, searchKey, order, random, tag);

        pageList.setList(list);
        pageList.setStartPageNo(offset);
        pageList.setPageSize(pageSize);
        pageList.setTotalCount(total);
        pageList.setTotalPageCount(totalPage);
        return pageList;
    }

    @Override
    public int total(Article article, String searchKey) {
        return dao.total(article, searchKey);
    }
}