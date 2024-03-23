package com.Fanbbs.service;
import java.util.List;
import com.Fanbbs.entity.*;
import com.Fanbbs.common.PageList;
public interface OrderService {
    /**
     * [新增]
     **/
    int insert(Order order);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Order> list);

    /**
     * [更新]
     **/
    int update(Order order);

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
    List<Order> selectList (Order order);

    /**
     * [分页条件查询]
     **/
    PageList<Order> selectPage (Order order, Integer page, Integer pageSize, String searchKey, String orderKey);

    /**
     * [总量查询]
     **/
    int total(Order order);
}
