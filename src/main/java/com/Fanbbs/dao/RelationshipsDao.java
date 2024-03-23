package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoRelationshipsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface RelationshipsDao {

    /**
     * [新增]
     **/
    int insert(Relationships relationships);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Relationships> list);

    /**
     * [更新]
     **/
    int update(Relationships relationships);

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
    List<Relationships> selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Relationships> selectList (Relationships relationships);

    /**
     * [分页条件查询]
     **/
    List<Relationships> selectPage (@Param("relationships") Relationships relationships, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Relationships relationships);
}
