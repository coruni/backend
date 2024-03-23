package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface CommentLikeDao {

    /**
     * [新增]
     **/
    int insert(CommentLike commentLike);


    /**
     * [更新]
     **/
    int update(CommentLike commentLike);

    /**
     * [删除]
     **/
    int delete(Object key);


    /**
     * [主键查询]
     **/
    CommentLike selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<CommentLike> selectList (CommentLike commentLike);

    /**
     * [分页条件查询]
     **/
    List<CommentLike> selectPage (@Param("CommentLike") CommentLike commentLike, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(CommentLike commentLike);
}
