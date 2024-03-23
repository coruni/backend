package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoShoptypeService
 * @author shoptype
 * @date 2023/07/10
 */
public interface ShoptypeService {

    /**
     * [新增]
     **/
    int insert(Shoptype shoptype);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Shoptype> list);

    /**
     * [更新]
     **/
    int update(Shoptype shoptype);

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
    Shoptype selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Shoptype> selectList (Shoptype shoptype);

    /**
     * [分页条件查询]
     **/
    PageList<Shoptype> selectPage (Shoptype shoptype, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Shoptype shoptype);
}
