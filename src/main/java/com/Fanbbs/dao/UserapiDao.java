package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUserapiDao
 * @author buxia97
 * @date 2022/01/10
 */
@Mapper
public interface UserapiDao {

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
    int batchDelete(List<Object> list);

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
    List<Userapi> selectPage (@Param("userapi") Userapi userapi, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Userapi userapi);
}
