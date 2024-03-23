package com.Fanbbs.dao;

import com.Fanbbs.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * dao层接口
 * TypechoViolationDao
 * @author buxia97
 * @date 2023/01/03
 */
@Mapper
public interface ViolationDao {

    /**
     * [新增]
     **/
    int insert(Violation violation);

    /**
     * [批量新增]
     **/
    int batchInsert(List<Violation> list);

    /**
     * [更新]
     **/
    int update(Violation violation);

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
    Violation selectByKey(Object key);

    /**
     * [条件查询]
     **/
    List<Violation> selectList (Violation violation);

    /**
     * [分页条件查询]
     **/
    List<Violation> selectPage (@Param("violation") Violation violation, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * [总量查询]
     **/
    int total(Violation violation);
}
