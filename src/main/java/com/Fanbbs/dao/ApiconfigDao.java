package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoApiconfigDao
 * @author apiconfig
 * @date 2022/04/28
 */
@Mapper
public interface ApiconfigDao {

    /**
     * [新增]
     **/
    int insert(Apiconfig apiconfig);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Apiconfig> list);

    /**
     * [更新]
     **/
    int update(Apiconfig apiconfig);

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
    Apiconfig selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Apiconfig> selectList (Apiconfig apiconfig);

    /**
     * [分页条件查询]
     **/
    List<Apiconfig> selectPage (@Param("Apiconfig") Apiconfig apiconfig, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Apiconfig apiconfig);
}
