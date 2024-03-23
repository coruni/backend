package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExchangeDao {
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
    int batchDelete(List<Object> list);

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
    List<Exchange> selectPage (@Param("exchange") Exchange exchange, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(@Param("exchange") Exchange exchange, @Param("searchKey") String searchKey);
}
