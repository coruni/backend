package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoFanService
 * @author buxia97
 * @date 2023/01/03
 */
public interface FanService {

    /**
     * [新增]
     **/
    int insert(Fan fan);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Fan> list);

    /**
     * [更新]
     **/
    int update(Fan fan);

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
    Fan selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Fan> selectList (Fan fan);

    /**
     * [分页条件查询]
     **/
    PageList<Fan> selectPage (Fan fan, Integer page, Integer pageSize);

    /**
     * [分页条件查询]
     **/
    PageList<Fan> selectUserPage (Fan fan, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Fan fan);
}
