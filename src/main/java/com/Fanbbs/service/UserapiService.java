package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoUserapiService
 * @author buxia97
 * @date 2022/01/10
 */
public interface UserapiService {

    /**
     * [新增]
     **/
    int insert(Userapi userapi);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Userapi> list);

    /**
     * [更新]
     **/
    int update(Userapi userapi);

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
    Userapi selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Userapi> selectList (Userapi userapi);

    /**
     * [分页条件查询]
     **/
    PageList<Userapi> selectPage (Userapi userapi, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Userapi userapi);
}
