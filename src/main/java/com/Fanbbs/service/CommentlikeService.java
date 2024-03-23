package com.Fanbbs.service;
import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

public interface CommentlikeService {
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
    PageList<CommentLike> selectPage (CommentLike commentLike, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(CommentLike commentLike);
}
