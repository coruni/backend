package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface AdsDao {

    /**
     * [新增]
     **/
    int insert(Ads ads);


    /**
     * [更新]
     **/
    int update(Ads ads);

    /**
     * [删除]
     **/
    int delete(Object key);


    /**
     * [主键查询]
     **/
    Ads selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Ads> selectList (Ads ads);

    /**
     * [分页条件查询]
     **/
    List<Ads> selectPage (@Param("Ads") Ads ads, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey);

    /**
     * [总量查询]
     **/
    int total(Ads ads);
}
