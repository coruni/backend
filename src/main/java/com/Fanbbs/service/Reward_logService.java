package com.Fanbbs.service;

import com.Fanbbs.common.PageList;
import com.Fanbbs.entity.Reward_log;

import java.util.List;

/**
 * 业务层
 * TypechoReward_logService
 * @author reward_log
 * @date 2022/09/06
 */
public interface Reward_logService {

    /**
     * [新增]
     **/
    int insert(Reward_log reward_log);
    /**
     * [更新]
     **/
    int update(Reward_log reward_log);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    Reward_log selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Reward_log> selectList (Reward_log reward_log);

    /**
     * [分页条件查询]
     **/
    PageList<Reward_log> selectPage (Reward_log reward_log, Integer page, Integer pageSize, String searchKey);

    /**
     * [总量查询]
     **/
    int total(Reward_log reward_log);
}
