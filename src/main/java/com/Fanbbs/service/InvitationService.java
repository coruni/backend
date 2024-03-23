package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoInvitationService
 * @author invitation
 * @date 2022/05/03
 */
public interface InvitationService {

    /**
     * [新增]
     **/
    int insert(Invitation invitation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Invitation> list);

    /**
     * [更新]
     **/
    int update(Invitation invitation);

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
    Invitation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Invitation> selectList (Invitation invitation);

    /**
     * [分页条件查询]
     **/
    PageList<Invitation> selectPage (Invitation invitation, Integer page, Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Invitation invitation);
}
