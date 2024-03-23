package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoPaylogService
 * @author buxia97
 * @date 2022/02/07
 */
public interface PaylogService {

    /**
     * [新增]
     **/
    int insert(Paylog paylog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Paylog> list);

    /**
     * [更新]
     **/
    int update(Paylog paylog);

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
    Paylog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Paylog> selectList (Paylog paylog);

    /**
     * [分页条件查询]
     **/
    PageList<Paylog> selectPage (Paylog paylog, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Paylog paylog);
}
