package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoPaylogDao
 * @author buxia97
 * @date 2022/02/07
 */
@Mapper
public interface PaylogDao {

    /**
     * [新增]
     **/
    int insert(Paylog paylog);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Paylog> list);

    /**
     * [更新]
     **/
    int update(Paylog paylog);

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
    Paylog selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Paylog> selectList (Paylog paylog);

    /**
     * [分页条件查询]
     **/
    List<Paylog> selectPage (@Param("paylog") Paylog paylog, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Paylog paylog);
}
