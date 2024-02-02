package com.TypeApi.dao;

import com.TypeApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HomepageDao {
    /**
     * [新增]
     **/
    int insert(Homepage homepage);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Homepage> list);

    /**
     * [更新]
     **/
    int update(Homepage homepage);

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
    Homepage selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Homepage> selectList(Homepage homepage);

    /**
     * [分页条件查询]
     **/
    List<Homepage> selectPage(@Param("homepage") Homepage homepage, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(Homepage homepage);

}

