package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoPaykeyDao
 * @author paykey
 * @date 2022/04/20
 */
@Mapper
public interface PaykeyDao {

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
    int batchDelete(List<Object> list);

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
    List<Paykey> selectPage (@Param("paykey") Paykey paykey, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey);

    /**
     * [总量查询]
     **/
    int total(Paykey paykey);
}
