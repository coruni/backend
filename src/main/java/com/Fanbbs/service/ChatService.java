package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoChatService
 * @author buxia97
 * @date 2023/01/10
 */
public interface ChatService {

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
    int batchDelete(List<Object> keys);

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
    PageList<Chat> selectPage (Chat chat, Integer page, Integer pageSize, String order, String searchKey);

    /**
     * [总量查询]
     **/
    int total(Chat chat);
}
