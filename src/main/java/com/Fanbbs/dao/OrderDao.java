package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/***
 * Dao层
 * 订单
 * Author Maplene
 */

@Mapper
public interface OrderDao {

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
    int batchDelete(List<Object> list);

    /**
     * [主键查询]
     **/
    Order selectByKey(Object key);

    /**
     * [订单查询]
     **/
    Order selectByOrder(Object orders);

    /**
     * [条件查询]
     **/
    List<Order> selectList(Order order);

    /**
     * [分页条件查询]
     **/
    List<Order> selectPage(@Param("order") Order order, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("orderKey") String orderKey);

    /**
     * [总量查询]
     **/
    int total(Order order);
}
