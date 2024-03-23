package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoMetasService
 * @author buxia97
 * @date 2021/11/29
 */
public interface CategoryService {

    /**
     * [新增]
     **/
    int insert(Category category);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Category> list);

    /**
     * [更新]
     **/
    int update(Category category);

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
    Category selectByKey(Object key);

    /**
     * [slug查询]
     **/
    Category selectBySlug(Object slug);

    /**
     * [条件查询]
     **/
    List<Category> selectList (Category category);

    /**
     * [分页条件查询]
     **/
    PageList<Category> selectPage (Category category, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Category category);
}
