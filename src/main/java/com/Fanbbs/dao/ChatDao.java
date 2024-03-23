package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoChatDao
 * @author buxia97
 * @date 2023/01/10
 */
@Mapper
public interface ChatDao {

    /**
     * [新增]
     **/
    int insert(Chat chat);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Chat> list);

    /**
     * [更新]
     **/
    int update(Chat chat);

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
    Chat selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Chat> selectList (Chat chat);

    /**
     * [分页条件查询]
     **/
    List<Chat> selectPage (@Param("chat") Chat chat, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("order") String order, @Param("searchKey") String searchKey);

    /**
     * [总量查询]
     **/
    int total(Chat chat);
}
