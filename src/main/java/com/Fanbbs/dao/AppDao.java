package com.Fanbbs.dao;

import com.Fanbbs.entity.App;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoAppDao
 * @author vips
 * @date 2023/06/09
 */
@Mapper
public interface AppDao {

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
    int batchDelete(List<Object> list);

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
    List<App> selectPage (@Param("app") App app, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(App app);
}
