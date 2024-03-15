package com.TypeApi.service;
import java.util.List;
import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;

public interface ExchangeService {
    /**
     * [新增]
     **/
    int insert(Exchange exchange);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Exchange> list);

    /**
     * [更新]
     **/
    int update(Exchange exchange);

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
    Exchange selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Exchange> selectList (Exchange exchange);

    /**
     * [分页条件查询]
     **/
    PageList<Exchange> selectPage (Exchange exchange, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Exchange exchange, String searchKey);
}
