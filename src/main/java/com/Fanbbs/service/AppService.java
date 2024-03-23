package com.Fanbbs.service;

import com.Fanbbs.common.PageList;
import com.Fanbbs.entity.App;

import java.util.List;

/**
 * 业务层
 * TypechoAppService
 * @author vips
 * @date 2023/06/09
 */
public interface AppService {

    /**
     * [新增]
     **/
    int insert(App app);

    /**
     * [批量新增]
     **/
    int batchInsert(List<App> list);

    /**
     * [更新]
     **/
    int update(App app);

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
    App selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<App> selectList (App app);

    /**
     * [分页条件查询]
     **/
    PageList<App> selectPage (App app, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(App app);
}
