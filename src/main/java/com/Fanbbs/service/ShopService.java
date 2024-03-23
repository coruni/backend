package com.Fanbbs.service;

import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;

/**
 * 业务层
 * TypechoShopService
 * @author buxia97
 * @date 2022/01/27
 */
public interface ShopService {

    /**
     * [新增]
     **/
    int insert(Shop shop);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Shop> list);

    /**
     * [更新]
     **/
    int update(Shop shop);

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
    Shop selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Shop> selectList (Shop shop);

    /**
     * [分页条件查询]
     **/
    PageList<Shop> selectPage (Shop shop, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Shop shop, String searchKey);
}
