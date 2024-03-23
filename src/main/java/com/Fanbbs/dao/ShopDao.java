package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoShopDao
 * @author buxia97
 * @date 2022/01/27
 */
@Mapper
public interface ShopDao {

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
    int batchDelete(List<Object> list);

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
    List<Shop> selectPage (@Param("shop") Shop shop, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(@Param("shop") Shop shop, @Param("searchKey") String searchKey);
}
