package com.TypeApi.service;

import java.util.List;
import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;
/**
 * 业务层
 * TypechoPaykeyService
 * @author paykey
 * @date 2022/04/20
 */
public interface PaykeyService {

    /**
     * [新增]
     **/
    int insert(Paykey paykey);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Paykey> list);

    /**
     * [更新]
     **/
    int update(Paykey paykey);

    /**
     * [删除]
     **/
    int delete(Object key);

    /***
     * [条件删除]
     */
    int typeDelete(Object type);

    /**
     * [批量删除]
     **/
    int batchDelete(List<Object> keys);

    /**
     * [主键查询]
     **/
    Paykey selectByKey(Object key);

    /***
     * [数值查询]
     */
    Paykey selectByCard(Object card);

    /**
     * [条件查询]
     **/
    List<Paykey> selectList (Paykey paykey);

    /**
     * [分页条件查询]
     **/
    PageList<Paykey> selectPage (Paykey paykey, Integer page, Integer pageSize, String searchKey);

    /**
     * [总量查询]
     **/
    int total(Paykey paykey);
}
