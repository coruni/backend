package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoFanDao
 * @author buxia97
 * @date 2023/01/03
 */
@Mapper
public interface FanDao {

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
    int batchDelete(List<Object> list);

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
    List<Fan> selectPage (@Param("fan") Fan fan, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [分页条件查询]
     **/
    List<Fan> selectUserPage (@Param("fan") Fan fan, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Fan fan);
}
