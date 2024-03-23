package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoChatMsgService
 * @author buxia97
 * @date 2023/01/11
 */
public interface ChatMsgService {

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
    int batchDelete(List<Object> keys);

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
    PageList<ChatMsg> selectPage (ChatMsg chatMsg, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(ChatMsg chatMsg);
}
