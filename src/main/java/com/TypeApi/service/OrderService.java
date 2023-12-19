package com.TypeApi.service;
import java.util.List;
import com.TypeApi.entity.*;
import com.TypeApi.common.PageList;
public interface OrderService {
    /**
     * [新增]
     **/
    int insert(Order Order);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Order> list);

    /**
     * [更新]
     **/
    int update(Order Order);

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
    Order selectByKey(Object key);

    /**
     * [orders查询]
     **/
    Order selectByOrder(Object orders);

    /**
     * [条件查询]
     **/
    List<Order> selectList (Order Order);

    /**
     * [分页条件查询]
     **/
    PageList<Order> selectPage (Order Order, Integer page, Integer pageSize, String searchKey, String order);

    /**
     * [总量查询]
     **/
    int total(Order Order);
}