package com.TypeApi.dao;

import com.TypeApi.entity.Reward_log;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface Reward_logDao {

    /**
     * [新增]
     **/
    int insert(Reward_log reward_log);


    /**
     * [更新]
     **/
    int update(Reward_log reward_log);

    /**
     * [删除]
     **/
    int delete(Object key);


    /**
     * [主键查询]
     **/
    Reward_log selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Reward_log> selectList (Reward_log reward_log);

    /**
     * [分页条件查询]
     **/
    List<Reward_log> selectPage (@Param("reward_log") Reward_log reward_log, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey);

    /**
     * [总量查询]
     **/
    int total(Reward_log reward_log);
}
