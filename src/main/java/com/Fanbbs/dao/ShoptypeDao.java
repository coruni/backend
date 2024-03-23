package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoShoptypeDao
 * @author shoptype
 * @date 2023/07/10
 */
@Mapper
public interface ShoptypeDao {

    /**
     * [新增]
     **/
    int insert(Shoptype shoptype);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Shoptype> list);

    /**
     * [更新]
     **/
    int update(Shoptype shoptype);

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
    Shoptype selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Shoptype> selectList (Shoptype shoptype);

    /**
     * [分页条件查询]
     **/
    List<Shoptype> selectPage (@Param("shoptype") Shoptype shoptype, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(Shoptype shoptype);
}
