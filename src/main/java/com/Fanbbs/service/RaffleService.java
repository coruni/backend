package com.Fanbbs.service;

import com.Fanbbs.common.PageList;
import com.Fanbbs.entity.Raffle;

import java.util.List;

/**
 * 业务层
 * TypechoRaffleService
 * @author Raffle
 * @date 2022/09/06
 */
public interface RaffleService {

    /**
     * [新增]
     **/
    int insert(Raffle raffle);
    /**
     * [更新]
     **/
    int update(Raffle raffle);

    /**
     * [删除]
     **/
    int delete(Object key);

    /**
     * [主键查询]
     **/
    Raffle selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Raffle> selectList (Raffle raffle);

    /**
     * [分页条件查询]
     **/
    PageList<Raffle> selectPage (Raffle raffle, Integer page, Integer pageSize, String searchKey);

    /**
     * [总量查询]
     **/
    int total(Raffle raffle);
}
