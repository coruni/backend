package com.Fanbbs.service;
import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

public interface RankService {
    /**
     * [新增]
     **/
    int insert(Rank rank);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Rank> list);

    /**
     * [更新]
     **/
    int update(Rank rank);

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
    Rank selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Rank> selectList (Rank rank);

    /**
     * [分页条件查询]
     **/
    PageList<Rank> selectPage (Rank rank, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Rank rank, String searchKey);
}
