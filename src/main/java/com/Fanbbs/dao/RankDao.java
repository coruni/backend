package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface RankDao {
    /**
     * [新增]
     **/
    int insert(Rank rank);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Rank> list);

    /**
     * [更新]
     **/
    int update(Rank rank);

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
    Rank selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Rank> selectList (Rank rank);

    /**
     * [分页条件查询]
     **/
    List<Rank> selectPage (@Param("rank") Rank rank, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order);

    /**
     * [总量查询]
     **/
    int total(@Param("rank") Rank rank, @Param("searchKey") String searchKey);
}
