package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoInboxDao
 * @author inbox
 * @date 2022/12/29
 */
@Mapper
public interface InboxDao {

    /**
     * [新增]
     **/
    int insert(Inbox inbox);

    /**
     * [更新]
     **/
    int update(Inbox inbox);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    Inbox selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Inbox> selectList (Inbox inbox);

    /**
     * [分页条件查询]
     **/
    List<Inbox> selectPage (@Param("inbox") Inbox inbox, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Inbox inbox);
}
