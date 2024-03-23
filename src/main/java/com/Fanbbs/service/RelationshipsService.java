package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoRelationshipsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface RelationshipsService {

    /**
     * [新增]
     **/
    int insert(Relationships relationships);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Relationships> list);

    /**
     * [更新]
     **/
    int update(Relationships relationships);

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
    List<Relationships>  selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Relationships> selectList (Relationships relationships);

    /**
     * [分页条件查询]
     **/
    PageList<Relationships> selectPage (Relationships relationships, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Relationships relationships);
}
