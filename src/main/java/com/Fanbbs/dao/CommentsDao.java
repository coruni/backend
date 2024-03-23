package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoCommentsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface CommentsDao {

    /**
     * [新增]
     **/
    int insert(Comments comments);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Comments> list);

    /**
     * [更新]
     **/
    int update(Comments comments);

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
    Comments selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Comments> selectList (Comments comments);

    /**
     * [分页条件查询]
     **/
    List<Comments> selectPage (@Param("comments") Comments comments, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(@Param("comments") Comments comments, @Param("searchKey") String searchKey);
}
