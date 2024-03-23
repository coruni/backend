package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoContentsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface ArticleService {

    /**
     * [新增]
     **/
    int insert(Article article);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Article> list);

    /**
     * [更新]
     **/
    int update(Article article);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    Article selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Article> selectList (Article article);

    /**
     * [分页条件查询]
     **/
    PageList<Article> selectPage (Article article, Integer page, Integer pageSize, String searchKey, String order, Integer random, Integer tag);

    /**
     * [总量查询]
     **/
    int total(Article article, String searchKey);
}
