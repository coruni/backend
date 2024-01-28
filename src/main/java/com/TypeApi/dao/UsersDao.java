package com.TypeApi.dao;

import com.TypeApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoUsersDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface UsersDao {

    /**
     * [新增]
     **/
    int insert(Users users);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Users> list);

    /**
     * [更新]
     **/
    int update(Users users);

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
    Users selectByKey(Object key);



    /**
     * [条件查询]
     **/
    List<Users> selectList (Users users);

    /**
     * [分页条件查询]
     **/
    List<Users> selectPage (@Param("users") Users users, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order,@Param("random") Integer random);

    /**
     * [总量查询]
     **/
    int total(@Param("users") Users users, @Param("searchKey") String searchKey);
}
