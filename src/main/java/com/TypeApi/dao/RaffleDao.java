package com.TypeApi.dao;

import com.TypeApi.entity.Raffle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface RaffleDao {

    /**
     * [新增]
     **/
    int insert(Raffle raffle);


    /**
     * [更新]
     **/
    int update(Raffle raffle);

    /**
     * [删除]
     **/
    int delete(Object key);


    /**
     * [主键查询]
     **/
    Raffle selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Raffle> selectList (Raffle raffle);

    /**
     * [分页条件查询]
     **/
    List<Raffle> selectPage (@Param("raffle") Raffle raffle, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey);

    /**
     * [总量查询]
     **/
    int total(Raffle raffle);
}
