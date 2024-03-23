package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoCommentsService
 * @author buxia97
 * @date 2021/11/29
 */
public interface CommentsService {

    /**
     * [新增]
     **/
    int insert(Comments comments);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Comments> list);

    /**
     * [更新]
     **/
    int update(Comments comments);

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
    Comments selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Comments> selectList (Comments comments);

    /**
     * [分页条件查询]
     **/
    PageList<Comments> selectPage (Comments comments, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Comments comments, String searchKey);
}
