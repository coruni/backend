package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoChatMsgDao
 * @author buxia97
 * @date 2023/01/11
 */
@Mapper
public interface ChatMsgDao {

    /**
     * [新增]
     **/
    int insert(ChatMsg chatMsg);

    /**
     * [批量新增]
     **/
    int batchInsert(List<ChatMsg> list);

    /**
     * [更新]
     **/
    int update(ChatMsg chatMsg);

    /**
     * [删除]
     **/
    int delete(Object key);
    int deleteMsg(Object key);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> list);

    /**
     * [主键查询]
     **/
    ChatMsg selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<ChatMsg> selectList (ChatMsg chatMsg);

    /**
     * [分页条件查询]
     **/
    List<ChatMsg> selectPage (@Param("chatMsg") ChatMsg chatMsg, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(ChatMsg chatMsg);
}
