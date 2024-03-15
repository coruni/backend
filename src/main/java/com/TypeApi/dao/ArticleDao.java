package com.TypeApi.dao;

import com.TypeApi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoContentsDao
 * @author buxia97
 * @date 2021/11/29
 */
@Mapper
public interface ArticleDao {

    /**
     * [新增]
     **/
    int insert(Article article);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Article> list);

    /**
     * [更新]
     **/
    int update(Article article);

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
    Article selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Article> selectList (Article article);

    /**
     * [分页条件查询]
     **/
    List<Article> selectPage (@Param("article") Article article, @Param("page") Integer page, @Param("pageSize") Integer pageSize, @Param("searchKey") String searchKey, @Param("order") String order, @Param("random") Integer random,@Param("tag") Integer tag);

    /**
     * [总量查询]
     **/
    int total(@Param("article") Article article, @Param("searchKey") String searchKey);

}
