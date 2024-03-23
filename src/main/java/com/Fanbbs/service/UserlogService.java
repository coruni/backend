package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoUserlogService
 * @author buxia97
 * @date 2022/01/06
 */
public interface UserlogService {

    /**
     * [新增]
     **/
    int insert(Userlog userlog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Userlog> list);

    /**
     * [更新]
     **/
    int update(Userlog userlog);

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
    Userlog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Userlog> selectList (Userlog userlog);

    /**
     * [分页条件查询]
     **/
    PageList<Userlog> selectPage (Userlog userlog, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Userlog userlog);
}
