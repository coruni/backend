package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoInboxService
 * @author inbox
 * @date 2022/12/29
 */
public interface InboxService {

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
    PageList<Inbox> selectPage (Inbox inbox, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Inbox inbox);
}
